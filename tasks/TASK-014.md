# TASK-014: Final QA & Release Readiness Audit — Offline-only v1.0.0

## Scope

Audit the complete production source and regression suite for Transaction CRUD,
Search/Filter, Home/Dashboard, Statistics, Monthly Budget, Wallet, Navigation and
Settings. Check financial/date boundaries, Room v2/migration, preferences, reactive
state, localization/theme, offline dependencies, permissions, performance risks,
release signing, hooks and CI. Fix confirmed bugs only; preserve storage identity,
schema and existing data. No feature additions, architecture changes, destructive
migration, data clearing, commit, push or release publication.

Final Release Verification continues on the existing working tree. Do not create
a keystore, read/change signing passwords, configure signing, commit/push, or
create a tag/release. Inspect the available artifact and, when supplied, the
user-created Signed APK using public signatures only. Mark artifact verification
complete when public signature/manifest evidence is recorded; keep the signed
upgrade open until a higher-version APK with the same signer is tested. An
IDE-signed APK does not require automatic Gradle signing configuration.

Signed Upgrade continuation (2026-10-28): the user now authorizes increasing
versionCode 2 → 3, keeping versionName 1.0.0, **only after** the existing signed
v2 APK is verified and archived separately. Build unsigned v3 with the completed
TASK-015 icon; the user signs it in Android Studio with the same Release key.
Prepare v2 sample data and stable before/after snapshots on a dedicated AVD.
Do not run instrumentation on that AVD. Earlier no-version-bump statements below
describe the previous scope; no keystore/password access or signing configuration
change is authorized. [The runbook](../docs/SIGNED_UPGRADE_v2_v3.md) defines the steps.

## Observable acceptance criteria

- Initial Git status/diff and repository/project/architecture/design/task guidance reviewed.
- Confirmed findings identify severity, affected files, cause and disposition.
- Focused regression tests reproduce important fixes and protect migration/preferences.
- Run `assembleDebug`, `assembleRelease`, `testDebugUnitTest`, `lintDebug`, build
  instrumentation APK, and execute isolated device/migration tests where supported.
- Check the actual release APK/signing report; an unsigned build is not an installable release.
- Compare existing Room records, relations, sequence and budget/appearance preferences
  across `adb install -r` and cold launch, without uninstalling or clearing storage.
- Record actual test counts, failures/retries, environment limits and manual release gates.
- For the continuation, distinguish a fresh unit execution from `UP-TO-DATE`,
  preserve every failed/interrupted instrumentation attempt, compare new data
  snapshots, and report API 26 boot attempts without inferring a runtime pass.
- Update README/ROADMAP and issue a justified Ready / Not Ready decision.
- For signed upgrade: verify/archive v2 without overwriting it; verify v3 has
  package/versionName unchanged, code 3 and the same public certificate before
  `adb install -r`. Compare Room rows/IDs/identity/schema/sequence/foreign keys,
  wallet balances, preferences and locale before install, after install and after
  cold launch; inspect UI totals and the new launcher icon. Missing signed v3 or
  device evidence remains INCOMPLETE, regardless of host-tool test results.

## Results

Latest signed upgrade execution (2026-10-28): **PASS v2 → v3 on the dedicated
API 35 AVD**, with original/preflight baseline matching before install, successful
`adb install -r`, both post-install and post-cold-launch data comparisons passing,
matching UI values and the purple wallet icon on Home Screen/App Drawer. See the
execution result at the end of this task and the [runbook](../docs/SIGNED_UPGRADE_v2_v3.md).
Earlier waiting-for-v3 statements are historical. API 26 and remote CI remain open.

See [the final audit report](../docs/RELEASE_AUDIT_v1.0.0.md) for evidence,
fixes and remaining release gates. Historical verification in TASK-013 does not
substitute for this audit's current-source checks.

Latest continuation: Debug/unsigned Release/test APK build gates passed; fresh
unit execution passed 98 tests; lint has 0 errors, 14 warnings and 1 hint. An API 35
full attempt stopped at case 53 after 52 passes with an ANR; the focused retry
passed 2/2. A later uninterrupted full run passed 63/63 in 245.229 seconds.
After the PixelCopy test adjustment, a new uninterrupted full run on current test
source passed 63/63 in 527.54 seconds. Local financial/appearance snapshots matched
through the suite; latest Room/preferences snapshot SHA-256 is
`25d01cc3b8ac7857270f7665e93aa61d1fb2836f83546132a83292eecf6c070e`. Additional
API 26 QA fixtures using WHPX and different encryption/data-partition settings
still did not reach `sys.boot_completed`; no app install or migration test ran
there. Signed upgrade, API 26 and the
remaining manual gates keep the decision **Not Ready for distribution**. No
keystore, signing password, commit/push or release actions were performed.

Open-issues review at the time: the user reports an IDE-signed APK, but no such artifact was
found in the workspace; the installed APK was pulled read-only and matches Debug.
Remote CI run 40480894011 passed at HEAD 90d39062, covering Debug/unit/lint only;
it does not certify the uncommitted TASK-014 changes. The workflow now builds the
unsigned Release variant and defines an API 26 emulator job for OfflineFlowTest
startup and WalletMigrationTest. The new workflow has not run remotely; do not
count its gates as passed until a run on the updated source succeeds. CI does not
sign APKs and needs no signing secret.

PixelCopy follow-up: in `SettingsFlowTest#systemThemeTracksDeviceWhileExplicitChoicesStayFixed`,
removed screenshot capture while global night mode changes and waited for the
applied status-bar appearance instead. `assembleDebugAndroidTest` passed; this
focused test passed four consecutive runs (8.219–10.785 seconds each), then the
full current suite passed 63/63 in one uninterrupted 527.54-second run. The
historical PixelCopy failure remains documented; the separate earlier ANR has no
captured root-cause trace.

API 26 startup/migration follow-up: additional fresh QA AVDs used WHPX. The
10 GB default fixture did not complete boot; with host `EncryptUserData` disabled,
guest logcat reported crypt-footer validation/write errors. A fresh 1 GB fixture
with default encryption appeared in ADB then went offline before boot completion.
Fixtures were stopped without wiping existing AVDs. The Room 1→2 migration test
passed as part of the API 35 suite only; API 26 startup and migration remain
unverified.

Accessibility follow-up: API 35 baseline was font scale 1.3, TalkBack disabled
(`enabled_accessibility_services=null`, `accessibility_enabled=0`), locale EN.
TalkBack was enabled temporarily, then disabled and the font scale restored to
1.3; all three settings were re-read and matched the baseline. The app opened into
a system permission screen, and UIAutomator returned a hierarchy for a different
package, so no TalkBack traversal or app font-scale 2.0 result was established.
The accessibility release gate remains open; existing large-text coverage does
not certify every screen at the maximum size, orientation, IME/date picker, or
navigation mode.

Performance review: the release APK is 9,246,543 bytes and release optimization
is disabled. Main-screen collectors currently perform transaction mapping and
summary/filter/balance calculations from `viewModelScope` on its main dispatcher;
the primary lists use lazy layouts with stable keys. Existing Debug-only cold
launch observations were 3,660 ms and 7,596 ms under different host load, with
one earlier idle PSS observation around 81,751 KB. These are diagnostics, not a
repeatable release benchmark or a performance budget. No optimization or code
refactor was made without representative data-size, frame-timing, and release
memory measurements. Runtime on a 16 KB page-size device remains unverified.

Final Build/Tests/Lint recheck (2026-10-27): Debug/Release and AndroidTest APK
builds passed. Fresh rerun completed 98/98 unit tests across 17 suites with no
failures/errors/skips; fresh lint reported 0 errors, 14 warnings, 1 hint. A new
API 35 instrumentation attempt did not finish: 42/63 test statuses passed, then
`SettingsFlowTest#androidPerAppLanguageChangeIsReflectedWithoutOverwritingIt`
stopped returning status and the emulator guest stopped responding. There was no
terminal summary, so this is incomplete, not a full pass or an assertion failure.
The Pixel_5 emulator was stopped without wiping app data; cold restart attempts
did not leave it online, so post-run locale/font/accessibility settings could not
be re-read. The earlier 63/63 uninterrupted run remains recorded separately. The
Release Audit now concludes **Not Ready for distribution** based on the open
signed APK/upgrade, API 26, remote CI and latest incomplete instrumentation gates.

Root-cause follow-up (2026-10-28; APK search status below is superseded by the
new artifact verification entry): the saved logcat for
`SettingsFlowTest#androidPerAppLanguageChangeIsReflectedWithoutOverwritingIt`
shows start at 23:11:07, runner finish at 23:11:22, and the next Settings test
starting at 23:11:22. It contains no ANR, fatal exception, or app exception, but
does show TalkBack/Android permission activities, 30–110 skipped frames, 1s
SurfaceSyncGroup timeouts, and system-server monitor contention. This log has no
JUnit XML/terminal status and cannot prove PASS. A later direct full run stalled
at the same test while guest ADB stopped responding; overlap with the previously
interrupted Gradle instrumentation process cannot be ruled out. Pixel_5 then
failed to stay online after cold-start attempts. An API 26 read-only AVD appeared
in ADB but went offline before `sys.boot_completed`. No confirmed app/test defect
was established, so no code/assertion/timeout change was made. The isolated locale
test, a subsequent full suite, API 26 migration run, and remote CI run remain
unverified. A read-only `gh run list` check on 2026-10-28 found no newer remote
workflow result: latest is run 40480894011 at the old HEAD 90d39062. Workspace
APK search found no Signed Release APK; only the prior unsigned artifact verified
as `DOES NOT VERIFY`. Release decision remains **Not Ready**.

Signed Release APK verification update (2026-10-28): found the user-created
artifact at `app/release/app-release.apk`. `app/build/outputs/apk/release/app-release.apk`
has identical size and SHA-256, confirming it is the same APK. `apksigner
verify --verbose --print-certs` passed with v2=true and one signer; certificate
DN is `C=VN, ST=Da Nang, L=Da Nang, O=Personal Projects, OU=Mobile Development,
CN=Nhi Nguyen`, certificate SHA-256
`bd649825a2e34febffbd4029f2062f824caec060b8266bb5cd7a6545902442b1`. Artifact
SHA-256: `AF23C7B1998250EDAB65DB233650AD495DF05666C3F8B41C6144865DD9474E69`.
APK metadata confirms `com.ngyennhi.expensemanager`, versionName `1.0.0`,
versionCode `2`, minSdk `26`, targetSdk `37`. Release signing verification is
**PASS** for this exact artifact. Signed upgrade/certificate continuity and data
retention remain **unverified**. API 26 startup/migration, instrumentation and a
remote CI run on the new workflow remain unverified; overall decision remains
**Not Ready**. No keystore or signing password was accessed.

For comparison, the local Debug APK reports certificate DN `C=US, O=Android,
CN=Android Debug`, SHA-256
`1f244570f4ed83462710d8400d8d88d8a4711f0a2767d2336535838374a55103`, which is
different from the release signer. This comparison does not establish upgrade
compatibility with the previously installed release certificate.

Final Release Verification continuation (2026-10-28): isolated
`SettingsFlowTest#androidPerAppLanguageChangeIsReflectedWithoutOverwritingIt`
passed 1/1 on Pixel_5/API 35; XML reports zero failures/errors/skips and 5.031s.
The first invocation was malformed by PowerShell argument parsing and did not run
any test. With the corrected quoted Gradle runner argument, the focused test passed
without changing its assertion or timeout. Then one full instrumentation runner
on `emulator-5554` passed **63/63 across 21 suites**, zero failures/errors/skips,
exit code 0; Gradle BUILD SUCCESSFUL in 4m01s. The suite included the locale test.
No overlapping instrumentation runner was used; API 26 was not part of this run.

Device side effect: the Expense Manager package was installed on Pixel_5 before
the focused run (`pm path` returned its APK), but absent after Gradle completed.
The Hilt Settings test uses an in-memory Room DB and `task010_test` preferences;
there was no pre-run snapshot of persistent app data, so this audit cannot claim
that such data was preserved. The full suite then ran on the same Pixel_5 after
the package was absent; Gradle installed it for instrumentation and it was absent
again afterward. No explicit wipe/clear or manual install/uninstall command was
issued.

Signed upgrade: workspace inventory contains only signed Release versionCode 2;
versionCode 1 candidates are signed by Android Debug certificate, not the Release
certificate. No versionCode-increased APK signed by the same Release key is
available. No version bump or signing operation was performed because that would
require using the key/password. Signed upgrade and Room/SharedPreferences data
retention remain **INCOMPLETE**.

API 26: started the existing `ExpenseManager_QA_API26_Plain` AVD without wiping.
ADB listed it, but guest shell/property calls did not return and boot completion
could not be established. No APK was installed and no migration/startup test ran;
status **INCOMPLETE**.

CI: local workflow review confirms PR/push triggers for `main` and `develop`, a
Debug/unsigned Release/unit/lint job, and an API 26 startup/migration job with
artifact uploads even on failure. `gh run list` still shows latest success
40480894011 on old HEAD `90d39062`, so there is no remote result for this workflow
or working tree. After opening a PR, verify both jobs for its exact commit and
check branch protection requirements. No commit/push was made.

Overall: **Not Ready**. Locale test and full API 35 instrumentation are PASS;
signed upgrade/data preservation, API 26 and CI for the new workflow are
INCOMPLETE. No app/test source or feature changes were made.

## Signed Upgrade v2 → v3 continuation — 2026-10-28

The user explicitly authorized the versionCode increase after preserving the
existing signed v2. [Runbook and signing steps](../docs/SIGNED_UPGRADE_v2_v3.md).

| Gate | Current evidence | Status |
| --- | --- | --- |
| Existing signed v2 | `apksigner verify --verbose --print-certs` succeeds, one Release signer, v2 signature scheme. `aapt dump badging`: package `com.ngyennhi.expensemanager`, code 2/name 1.0.0. Certificate and APK SHA-256 match the earlier audit. | PASS |
| Preserve v2 before building | Copied to `app/release/upgrade-verification/v2/app-v1.0.0-vc2-release.apk`; reverified signature/hash and set read-only. SHA-256 `AF23C7B1998250EDAB65DB233650AD495DF05666C3F8B41C6144865DD9474E69` still matches after v3 build. Outside build output and ignored by Git. | PASS |
| Version and Release build | Only `versionCode = 2` → `3` in `app/build.gradle.kts`; name remains 1.0.0, signing configuration unchanged. `:app:assembleRelease --console=plain --max-workers=1`: BUILD SUCCESSFUL in 26s, 54 tasks (12 executed/42 up-to-date), including lintVitalRelease. | PASS |
| v3 packaged resources | `aapt` confirms code 3/name 1.0.0, package/minSdk/target unchanged. All 15 launcher WebP source hashes match APK content. All 138 files under `app/src/main` and `app/schemas` unchanged from the start of this continuation. | PASS |
| Dedicated AVD + v2 install | New `ExpenseManager_SignedUpgrade_API35`, `emulator-5558`, API 35 Google APIs. Initial boot exited; retry with host GPU/Vulkan disabled completed. Confirmed package absent before first install; verified archived v2 installed with `adb install -r` and cold-started successfully. Root used only on this AVD for read-only private-data capture. | PASS on retry |
| v2 baseline | UI-created Default/UpgradeCash/UpgradeBank wallets (IDs 1/2/3), three transactions (IDs 1/2/3 → wallets 2/2/3), budget 1,000,000 for 2026-10, explicit LIGHT/en. Wallet balances 0/1,375,000/1,800,000; income/expense/net 500,000/325,000/175,000; remaining budget 675,000. Private DB/WAL/preferences and UI screenshots saved separately. | PASS before upgrade |
| Baseline internal checks | UID 10209, schema 2, identity `ffda5e095f46485f8c26d51bae071165`; wallet/transaction sequence 3/3, foreign_key_check empty, integrity_check ok. Snapshot JSON SHA-256 `ab78459847ff55e63b93b7fe2d80c088a1bad11362117fda26df25606eb8385f`. | PASS before upgrade |
| Snapshot tooling | Standard-library Python helper and host-only checks, latest run 7/7 PASS. Reads copied SQLite/WAL; compares complete rows/IDs/relationships/sequence/balances, preference content, locale and UID. Does not install, clear storage or run instrumentation. | PASS for tooling |
| Signed v3 + actual upgrade | Unsigned v3 exists; user must sign it in Android Studio using the same Release key. No signed v3 supplied/verified yet, no v3 install performed, no post-upgrade snapshot/UI/icon evidence. | INCOMPLETE |

Unsigned v3 SHA-256:
`46381c677cd98b7134d089a60ff0314e12755976356df288b9a17edd6f5ee309`.
Preserved unsigned copy:
`app/release/upgrade-verification/v3-unsigned/app-v1.0.0-vc3-unsigned.apk`.
The public Release certificate fingerprint remains
`bd649825a2e34febffbd4029f2062f824caec060b8266bb5cd7a6545902442b1`.

Tool follow-ups: the first synthetic check exposed an unclosed local SQLite
connection during Windows fixture cleanup; fixed with explicit closing and reran.
The first on-device capture refused Android 15's `appId` field; added support for
both `appId`/`userId`, a storage-owner check and a host regression. Capture then
passed. Neither attempt changed app data. v2 is left stopped after snapshot on
the running dedicated AVD. Do not seed again or run instrumentation there.
The v2 App Drawer screenshot still shows the original Android template icon;
unsigned v3 contains the purple wallet icon. Actual icon replacement after the
signed upgrade remains INCOMPLETE.

No Room schema/app feature/signing change, keystore/password access, app data
deletion, uninstall, commit/push or release publication. Unit/Debug/full lint and
Android instrumentation were not rerun in this continuation. Overall release
decision remains **Not Ready**; signed upgrade, API 26 and remote CI remain open.

## Signed Upgrade execution — PASS API 35, 2026-10-28

Signed APK supplied by the user:
`app/release/upgrade-verification/v3-signed/release/app-release.apk`, SHA-256
`1DD171FB8BE0118A37B5C611B4162BBBECD608891B96E54FFBFB659B0E8BAD79`.
Reverified v2/v3 using public signatures and manifest metadata: package/name
unchanged, versionCode 2/3, signature scheme v2=true, one signer each, same
certificate SHA-256 `bd649825a2e34febffbd4029f2062f824caec060b8266bb5cd7a6545902442b1`.

Preflight on `ExpenseManager_SignedUpgrade_API35` / `emulator-5558` confirmed
boot completed, installed v2 matching the archived APK and UID 10209. Fresh
`preflight-v2` equals the original baseline byte-for-byte at snapshot JSON level
(SHA-256 `ab78459847ff55e63b93b7fe2d80c088a1bad11362117fda26df25606eb8385f`).
Only after this PASS, installed the supplied v3 with `adb install -r`: Success,
exit 0, versionCode 3/name 1.0.0 and UID 10209 retained.

Both `after-v3-install` and `after-v3-cold-launch` compare **PASS** against
`before-v2`: every wallet/transaction column, IDs and wallet references, balances,
Room user_version/identity/schema, sqlite_sequence, foreign keys, preferences and
locale unchanged. Three wallet IDs 1/2/3, transaction IDs 1/2/3 → wallets 2/2/3;
balances 0/1,375,000/1,800,000, total 3,175,000. Room remains schema 2, identity
`ffda5e095f46485f8c26d51bae071165`, sequences 3/3, no FK violations, integrity ok.
Budget 2026-10 remains 1,000,000, spent 325,000, remaining 675,000; theme LIGHT
and locale en retained.

Cold launch Status ok/LaunchState COLD; UI Home shows income/expense/net
500,000/325,000/175,000; Budget and wallet totals match; all three transaction
details show the original wallet/name/amount/note. Settings visibly selects
English and Light. Visual screenshots confirm the new purple wallet icon on
Home Screen and App Drawer; tapping it opens the app. Signed v3 contains all
15 source launcher WebP images. No launcher data clearing/reboot was needed.

Evidence under `app/release/upgrade-verification/run-v2-v3-20261028/`, ignored by
Git. `result.json` records PASS and all checks. Snapshot hashes after install /
after cold launch:
`8364d8529c43c2f0a766e0a3b6ca32b977ac4e4b916133c8fa49afb1bd9286e0` /
`9864a955c56e02af4172a4993fd3850368eae66e093a0c7cf46d10830f2520e4`.
Only WAL/SHM raw hashes change after cold launch; logical Room data is identical.

**Signed-upgrade gate: PASS for this API 35 fixture.** No FAIL in this execution.
API 26 startup/migration and remote CI remain **INCOMPLETE**; distribution stays
**Not Ready** pending those and remaining manual gates. No Gradle checks or
instrumentation were run in this runtime-only execution. No code/schema changes,
uninstall, wipe/clear, keystore/password access, commit/push or release publication.
AVD now retains v3 with the sample data, stopped after the final capture.
