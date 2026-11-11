"""Capture/compare release app data on the dedicated, already-rooted QA AVD.

Uses only Python's standard library. Never installs, uninstalls, clears storage,
seeds data, roots ADB, or runs instrumentation. Capture force-stops this app so
the copied database/WAL/preferences form a stable snapshot; it leaves it stopped.
"""

import argparse
from contextlib import closing
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import re
import sqlite3
import subprocess
import tarfile
import xml.etree.ElementTree as ET

PACKAGE = "com.ngyennhi.expensemanager"
AVD = "ExpenseManager_SignedUpgrade_API35"


def adb_run(adb, serial, *args):
    result = subprocess.run(
        [adb, "-s", serial, *args], capture_output=True, timeout=30
    )
    if result.returncode:
        raise RuntimeError(result.stderr.decode("utf-8", errors="replace"))
    return result.stdout


def xml_value(node):
    children = [xml_value(child) for child in node]
    if node.tag in ("map", "set"):
        children.sort(key=lambda item: json.dumps(item, sort_keys=True))
    return {
        "tag": node.tag,
        "attributes": dict(sorted(node.attrib.items())),
        "text": node.text if not children and node.text is not None else "",
        "children": children,
    }


def package_metadata(text):
    version = re.search(r"\bversionCode=(\d+)", text)
    # Recent Android images print appId where older dumps used userId.
    identifier = re.search(r"\b(?:userId|appId)=(\d+)", text)
    if not version or not identifier:
        raise RuntimeError("Installed package metadata is missing version/UID")
    return int(version[1]), int(identifier[1])


def summarize(directory):
    database = directory / "databases/expense_manager.db"
    with closing(sqlite3.connect(database.resolve().as_uri() + "?mode=ro", uri=True)) as db:
        db.row_factory = sqlite3.Row

        def rows(query):
            return [dict(row) for row in db.execute(query)]

        wallets = rows("SELECT * FROM wallets ORDER BY id")
        transactions = rows("SELECT * FROM transactions ORDER BY id")
        room = {
            "user_version": db.execute("PRAGMA user_version").fetchone()[0],
            "identity": rows("SELECT * FROM room_master_table ORDER BY id"),
            "schema": rows(
                "SELECT type,name,tbl_name,sql FROM sqlite_master ORDER BY type,name"
            ),
            "wallets": wallets,
            "transactions": transactions,
            "sequence": rows("SELECT * FROM sqlite_sequence ORDER BY name"),
            "foreign_keys": rows("PRAGMA foreign_key_list(transactions)"),
            "foreign_key_check": rows("PRAGMA foreign_key_check"),
            "integrity_check": [row[0] for row in db.execute("PRAGMA integrity_check")],
        }
    if room["user_version"] != 2 or room["foreign_key_check"]:
        raise RuntimeError("Unexpected schema version or broken wallet references")
    if room["integrity_check"] != ["ok"]:
        raise RuntimeError("SQLite integrity check failed")
    balances = {wallet["id"]: wallet["opening_balance_vnd"] for wallet in wallets}
    for transaction in transactions:
        if transaction["type"] not in ("INCOME", "EXPENSE"):
            raise RuntimeError("Unexpected transaction type")
        sign = 1 if transaction["type"] == "INCOME" else -1
        balances[transaction["wallet_id"]] += sign * transaction["amount_vnd"]
    preferences = {
        path.name: xml_value(ET.parse(path).getroot())
        for path in sorted((directory / "shared_prefs").glob("*.xml"))
    }
    return {"room": room, "preferences": preferences, "balances_vnd": balances}


def capture(args):
    if args.out.exists():
        raise RuntimeError("Output already exists; preserve it and choose a new path")
    if not args.serial.startswith("emulator-"):
        raise RuntimeError("An explicit emulator serial is required")

    def call(*command):
        return adb_run(args.adb, args.serial, *command)

    avd = call("shell", "getprop", "ro.boot.qemu.avd_name").decode().strip()
    if avd != AVD:
        raise RuntimeError(f"Refusing capture on AVD {avd!r}; expected {AVD!r}")
    if call("shell", "getprop", "sys.boot_completed").strip() != b"1":
        raise RuntimeError("AVD has not completed boot")
    if call("shell", "id", "-u").strip() != b"0":
        raise RuntimeError("Use adb root on this dedicated Google APIs AVD first")
    package_info = call("shell", "dumpsys", "package", PACKAGE).decode()
    version, uid = package_metadata(package_info)
    if version != args.expected_version:
        raise RuntimeError("Installed package version/UID does not match expectation")
    owner = int(call("shell", "stat", "-c", "%u", f"/data/user/0/{PACKAGE}"))
    if owner != uid:
        raise RuntimeError("Private storage owner differs from the user-0 package UID")
    installed_path = call("shell", "pm", "path", PACKAGE).decode().strip()
    if "\n" in installed_path or not installed_path.startswith("package:/data/app/"):
        raise RuntimeError("Expected a single installed base APK")
    installed_hash = hashlib.sha256(
        call("exec-out", "cat", installed_path.removeprefix("package:"))
    ).hexdigest()
    if installed_hash != hashlib.sha256(args.apk.read_bytes()).hexdigest():
        raise RuntimeError("Installed APK differs from supplied verified artifact")

    call("shell", "am", "force-stop", PACKAGE)
    archive = call(
        "exec-out", "tar", "-C", f"/data/user/0/{PACKAGE}",
        "-cf", "-", "databases", "shared_prefs",
    )
    args.out.mkdir(parents=True)
    (args.out / "private-data.tar").write_bytes(archive)
    raw = args.out / "raw"
    with tarfile.open(fileobj=io.BytesIO(archive)) as tar:
        for member in tar:
            path = PurePosixPath(member.name)
            if path.is_absolute() or ".." in path.parts or not path.parts:
                raise RuntimeError("Unsafe archive member")
            if path.parts[0] not in ("databases", "shared_prefs"):
                raise RuntimeError("Unexpected archive member")
            if member.isdir():
                continue
            if not member.isfile():
                raise RuntimeError("Refusing non-regular archive member")
            target = raw.joinpath(*path.parts)
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(tar.extractfile(member).read())
    state = summarize(raw)
    state["locale"] = call(
        "shell", "cmd", "locale", "get-app-locales", PACKAGE, "--user", "0"
    ).decode().strip()
    snapshot = {
        "avd": avd, "serial": args.serial, "package": PACKAGE,
        "version_code": version, "uid": uid,
        "installed_apk_sha256": installed_hash,
        "state": state,
        "file_sha256": {
            path.relative_to(raw).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest()
            for path in sorted(raw.rglob("*")) if path.is_file()
        },
    }
    (args.out / "package.txt").write_text(package_info, encoding="utf-8")
    (args.out / "snapshot.json").write_text(
        json.dumps(snapshot, ensure_ascii=False, sort_keys=True, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Captured versionCode {version} to {args.out}; app left stopped")


def compare(args):
    before = json.loads((args.before / "snapshot.json").read_text(encoding="utf-8"))
    after = json.loads((args.after / "snapshot.json").read_text(encoding="utf-8"))
    if (before["version_code"], after["version_code"]) != (2, 3):
        raise RuntimeError("Expected v2 -> v3")
    for key in ("avd", "serial", "package", "uid", "state"):
        if before[key] != after[key]:
            raise RuntimeError(f"Retention mismatch: {key}; inspect snapshot JSON files")
    state = before["state"]
    if len(state["room"]["wallets"]) < 2 or len(state["room"]["transactions"]) < 3:
        raise RuntimeError("Insufficient baseline wallet/transaction samples")
    prefs = state["preferences"]
    budget = prefs.get("monthly_budgets.xml", {}).get("children", [])
    appearance = prefs.get("appearance.xml", {}).get("children", [])
    if not any(item["attributes"].get("name", "").startswith("monthly_budget:") for item in budget):
        raise RuntimeError("Missing saved monthly-budget baseline")
    if not any(item["attributes"].get("name") == "theme" for item in appearance):
        raise RuntimeError("Missing saved appearance baseline")
    print("PASS: same AVD/package/UID; Room schema, identity, rows, IDs, sequence,")
    print("wallet links, balances, preferences and locale retained across v2 -> v3.")
    print("Also verify public certificates, UI totals and launcher icon separately.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    command = commands.add_parser("capture")
    command.add_argument("--adb", required=True)
    command.add_argument("--serial", required=True)
    command.add_argument("--apk", type=Path, required=True)
    command.add_argument("--expected-version", type=int, choices=(2, 3), required=True)
    command.add_argument("--out", type=Path, required=True)
    command.set_defaults(action=capture)
    command = commands.add_parser("compare")
    command.add_argument("--before", type=Path, required=True)
    command.add_argument("--after", type=Path, required=True)
    command.set_defaults(action=compare)
    args = parser.parse_args()
    try:
        args.action(args)
    except (RuntimeError, OSError, sqlite3.Error, ET.ParseError, subprocess.TimeoutExpired) as error:
        parser.exit(1, f"INCOMPLETE/FAIL: {error}\n")


if __name__ == "__main__":
    main()
