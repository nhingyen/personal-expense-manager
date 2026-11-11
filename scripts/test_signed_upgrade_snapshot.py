"""Host-only checks with synthetic SQLite/WAL data; never contacts an emulator."""

import contextlib
import copy
import io
import json
from pathlib import Path
import sqlite3
import tempfile
from types import SimpleNamespace
import unittest
import xml.etree.ElementTree as ET

import signed_upgrade_snapshot as snapshot


class SnapshotChecks(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        raw = self.root / "raw"
        (raw / "databases").mkdir(parents=True)
        (raw / "shared_prefs").mkdir()
        self.db = sqlite3.connect(raw / "databases/expense_manager.db")
        self.addCleanup(self.db.close)
        self.db.execute("PRAGMA journal_mode=WAL")
        schema_path = Path(__file__).resolve().parents[1] / (
            "app/schemas/com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase/2.json"
        )
        schema = json.loads(schema_path.read_text(encoding="utf-8"))["database"]
        for entity in reversed(schema["entities"]):
            self.db.execute(entity["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
        for query in schema["setupQueries"]:
            self.db.execute(query)
        self.db.execute("PRAGMA user_version=2")
        self.db.executemany("INSERT INTO wallets VALUES(?,?,?,?)", [
            (1, "", "CASH", 0), (2, "Upgrade Cash", "CASH", 1_000_000),
        ])
        self.db.executemany("INSERT INTO transactions VALUES(?,?,?,?,?,?,?)", [
            (1, "INCOME", 200_000, "Salary", 20754, "one", 2),
            (2, "EXPENSE", 50_000, "Food", 20754, "two", 2),
            (3, "EXPENSE", 30_000, "Travel", 20754, "three", 1),
        ])
        self.db.commit()
        (raw / "shared_prefs/monthly_budgets.xml").write_text(
            '<map><long name="monthly_budget:2026-10" value="500000"/></map>', encoding="utf-8"
        )
        (raw / "shared_prefs/appearance.xml").write_text(
            '<map><string name="theme">DARK</string></map>', encoding="utf-8"
        )
        state = snapshot.summarize(raw)
        state["locale"] = "Locales for package are [en]"
        self.before = {
            "avd": snapshot.AVD, "serial": "emulator-5558", "package": snapshot.PACKAGE,
            "uid": 10123, "version_code": 2, "state": state,
        }
        self.after = copy.deepcopy(self.before)
        self.after["version_code"] = 3

    def compare(self, before=None, after=None):
        old = self.root / "before"
        new = self.root / "after"
        old.mkdir(exist_ok=True)
        new.mkdir(exist_ok=True)
        (old / "snapshot.json").write_text(json.dumps(before or self.before), encoding="utf-8")
        (new / "snapshot.json").write_text(json.dumps(after or self.after), encoding="utf-8")
        with contextlib.redirect_stdout(io.StringIO()):
            snapshot.compare(SimpleNamespace(before=old, after=new))

    def test_reads_committed_wal_and_computes_balances(self):
        state = self.before["state"]
        self.assertEqual({1: -30_000, 2: 1_150_000}, state["balances_vnd"])
        self.assertEqual(3, len(state["room"]["transactions"]))
        self.assertEqual([], state["room"]["foreign_key_check"])

    def test_equivalent_v2_v3_pass(self):
        self.compare()

    def test_changed_rows_ids_links_sequence_prefs_or_locale_rejected(self):
        for key in ("transactions", "wallets", "sequence", "identity"):
            with self.subTest(key=key):
                after = copy.deepcopy(self.after)
                after["state"]["room"][key] = []
                with self.assertRaises(RuntimeError):
                    self.compare(after=after)
        for key in ("preferences", "locale", "balances_vnd"):
            with self.subTest(key=key):
                after = copy.deepcopy(self.after)
                after["state"][key] = {}
                with self.assertRaises(RuntimeError):
                    self.compare(after=after)

    def test_changed_uid_or_wrong_version_rejected(self):
        for key, value in (("uid", 999), ("version_code", 2)):
            with self.subTest(key=key):
                after = copy.deepcopy(self.after)
                after[key] = value
                with self.assertRaises(RuntimeError):
                    self.compare(after=after)

    def test_insufficient_seed_rejected(self):
        before, after = copy.deepcopy(self.before), copy.deepcopy(self.after)
        for state in (before, after):
            state["state"]["room"]["transactions"] = []
        with self.assertRaises(RuntimeError):
            self.compare(before, after)

    def test_preference_order_is_semantic(self):
        old = ET.fromstring('<map><string name="a">x</string><long name="b" value="2"/></map>')
        new = ET.fromstring('<map><long value="2" name="b"/><string name="a">x</string></map>')
        self.assertEqual(snapshot.xml_value(old), snapshot.xml_value(new))

    def test_android_package_metadata_formats(self):
        for field in ("userId", "appId"):
            self.assertEqual((2, 10209), snapshot.package_metadata(
                f"    {field}=10209\n    versionCode=2 minSdk=26 targetSdk=37\n"
            ))
        with self.assertRaises(RuntimeError):
            snapshot.package_metadata("Unknown package")


if __name__ == "__main__":
    unittest.main()
