# TASK-013: Offline-only v1.0.0

> Current verification is superseded by [TASK-014](TASK-014.md) and the
> [final audit report](../docs/RELEASE_AUDIT_v1.0.0.md). The audit found the checked-in
> manifest/extraction rules still enabled default backup despite the historical
> verification claims below; TASK-014 corrects that configuration and adds packaged
> policy regression tests. Earlier device/build counts describe earlier runs only.

## Scope and acceptance criteria

Replace the optional authentication scope from TASK-011/TASK-012 with one local financial profile. Cloud Backup & Restore and Local Multi-account are Future Improvements, not release features.

- [x] Remove Firebase Authentication, Google Sign-In, credential picker, auth domain/data/DI/UI/tests, unused resources and Firebase build/CI configuration.
- [x] Launch directly into Home; Settings keeps language, appearance, Wallets and installed version without account or cloud controls.
- [x] Disable Android backup exports and exclude app storage from cloud backup/device transfer without deleting local files.
- [x] Keep application ID, `expense_manager.db`, Room schema v2, migration 1→2, IDs/relations and `monthly_budgets` preferences unchanged. Do not clear app data, uninstall the app, add destructive migration or introduce another production database.
- [x] Retain Transaction CRUD, Wallets, monthly Budget, Dashboard, Statistics and Settings; add an isolated offline launch/navigation regression.
- [x] Run Debug Build, Unit Tests and Lint; verify financial UI on an emulator and compare pre-update/post-update local financial data using an in-place APK update.
- [x] Update README, architecture, design and roadmap with current scope and actual verification; list remaining manual release checks. Do not commit or push.

## Verification

- Final Gradle command: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --console=plain --max-workers=1 --no-daemon --no-configuration-cache`: **passed**. Unit XML: **96 tests, 17 suites, 0 failures/errors/skips**. Lint: **0 errors, 16 warnings, 1 hint**. The first combined run also executed all 96 unit tests successfully.
- Two subsequent build attempts encountered local tooling failures: AGP/Hilt incremental transform could not find a generated factory; a rerun then hit a Windows lock on `classes.jar`. Regenerating outputs and stopping Gradle daemons resolved these; the final command above passed without changing financial code.
- The final working tree also disables Android backup/transfer exports in the manifest/extraction rules and removes the unused legacy backup XML. Rebuilt all four Gradle gates and reran the 11-test offline device suite after those configuration changes; both passed. Final manifest has `allowBackup=false`, `fullBackupContent=false` and no INTERNET permission. Source input hashes stayed unchanged throughout final verification.
- Pixel_5 / API 35, Wi-Fi and mobile data disabled: **11 selected instrumentation tests passed** through `adb shell am instrument -w -r`, installed with `adb install -r` / `adb install -r -t`. The suite ran OfflineFlowTest (1), TransactionFlowTest (2), WalletFlowTest wallet CRUD (1), HomeFlowTest startup/statistics/budget cases (4), and SettingsFlowTest locale/theme/recreation/version/captures (3). Test writes used in-memory Room and isolated/fake preference storage; production financial data was not used for CRUD tests.
- Production update verification: updated the already installed app from version 1.0/code 1 to 1.0.0/code 2 using `adb install -r`. The first-install timestamp stayed unchanged. Captured Room/preferences before update, immediately after install, after instrumentation, and after a production cold launch. All snapshots matched: **schema v2, 4 existing transactions, 1 wallet, 1 budget preference entry**, with identical field values, IDs, relations, autoincrement sequence, Room identity and appearance values. `PRAGMA foreign_key_check` returned no errors. No app uninstall, storage clear or replacement production database occurred.
- Read-only production UI smoke with network disabled: Home opened directly; Transactions, Statistics, Settings and Wallets rendered the retained data and returned to Home. Real production data captures stay in ignored local build artifacts. Wi-Fi was restored to its initial enabled state; mobile data remained disabled.
- Final merged manifest/APK inspection: no INTERNET permission, Firebase provider/classes, Credential Manager/Google identity classes, auth presentation classes or generated Firebase client resources. Room entities/DAOs/database/schema, financial repositories/ViewModels and Budget storage/module have no Git diff. VI/EN resource keys match; `git diff --check` passed.
- Refreshed VI/light and EN/dark Settings screenshots from isolated device captures and inspected them. Removed obsolete Login/Register captures. Local legacy service JSON was moved to ignored `app/build/offline-release/legacy-google-services.json`; it is not read by the build or packaged. Existing Firebase SDK files on the emulator were left untouched.
- Local evidence: `app/build/offline-release/` contains logical snapshots, read-only UI captures and `instrumentation.txt`; Gradle reports are under `app/build/reports/` and `app/build/test-results/`. These artifacts are ignored and may contain local data; they are not included in source control.
- **Still manual before publishing:** API 26 startup/upgrade, large-font/accessibility review and a release-signed upgrade with the intended production key. Only Debug build and API 35 device execution are claimed. CI configuration was updated locally; no remote CI run, commit or push was performed.

## Manual emulator checklist

1. On the old installed app, record existing transactions (including IDs, amount, date, note and wallet), wallet opening balances/balances, and saved monthly limits. Keep the application ID and signing key. Never uninstall, clear storage or wipe the emulator for this check.
2. Install the new APK with `adb install -r`; confirm version 1.0.0 (code 2), the same `expense_manager.db` at user_version 2 and unchanged existing records, wallet references and budget limits. Force-stop/relaunch and confirm them again.
3. Turn off Wi-Fi/mobile data, cold-launch and verify Home opens directly. Settings must have language, Wallets, appearance and About without login, Google, Guest or in-app backup/restore controls.
4. Using disposable test records in isolated test storage, create income/expense, edit amount/type/date/wallet, cancel an edit and test both cancellation/confirmation of deletion. Verify the list, Home totals/recent rows, Statistics/category charts and budget spent/remaining react correctly.
5. Create/edit a test wallet, check opening balance plus transactions, select it in a transaction and confirm that a nonempty/default wallet cannot be deleted. Delete only disposable test records/wallets.
6. Set/edit/remove a test monthly budget, change month, check reached/exceeded states and reopen the editor. Check VI/EN, Light/Dark/System, Wallets navigation and installed version; recreate the Activity and cold-launch again.
7. Before publishing, repeat startup/upgrade smoke on API 26, review large fonts and accessibility, and test a release-signed upgrade with the intended signing key. Record any checks not executed rather than inferring a pass from Debug tests.

## Changed files

`M`: modified; `D`: removed; `A`: added. Financial storage and calculation files are unchanged.

| Change | File |
| --- | --- |
| M | `.github/workflows/android-ci.yml` |
| M | `.gitignore` |
| M | `README.md` |
| M | `app/build.gradle.kts` |
| D | `app/google-services.example.json` |
| D | `app/src/androidTest/java/com/ngyennhi/expensemanager/presentation/AuthFlowTest.kt` |
| D | `app/src/androidTest/java/com/ngyennhi/expensemanager/presentation/AuthScreenTest.kt` |
| A | `app/src/androidTest/java/com/ngyennhi/expensemanager/presentation/OfflineFlowTest.kt` |
| M | `app/src/main/AndroidManifest.xml` |
| M | `app/src/main/java/com/ngyennhi/expensemanager/MainActivity.kt` |
| D | `app/src/main/java/com/ngyennhi/expensemanager/data/repository/FirebaseAuthRepository.kt` |
| D | `app/src/main/java/com/ngyennhi/expensemanager/di/AuthModule.kt` |
| D | `app/src/main/java/com/ngyennhi/expensemanager/domain/model/Auth.kt` |
| D | `app/src/main/java/com/ngyennhi/expensemanager/domain/repository/AuthRepository.kt` |
| D | `app/src/main/java/com/ngyennhi/expensemanager/domain/usecase/ValidateAuthCredentialsUseCase.kt` |
| M | `app/src/main/java/com/ngyennhi/expensemanager/presentation/ExpenseManagerApp.kt` |
| D | `app/src/main/java/com/ngyennhi/expensemanager/presentation/auth/AccountCard.kt` |
| D | `app/src/main/java/com/ngyennhi/expensemanager/presentation/auth/AuthScreen.kt` |
| D | `app/src/main/java/com/ngyennhi/expensemanager/presentation/auth/AuthViewModel.kt` |
| D | `app/src/main/java/com/ngyennhi/expensemanager/presentation/auth/GoogleCredentialPicker.kt` |
| M | `app/src/main/java/com/ngyennhi/expensemanager/presentation/navigation/AppDestination.kt` |
| M | `app/src/main/java/com/ngyennhi/expensemanager/presentation/settings/SettingsScreen.kt` |
| D | `app/src/main/res/drawable-nodpi/google_g.png` |
| M | `app/src/main/res/values-en/strings.xml` |
| M | `app/src/main/res/values/strings.xml` |
| D | `app/src/main/res/xml/backup_rules.xml` |
| M | `app/src/main/res/xml/data_extraction_rules.xml` |
| D | `app/src/test/java/com/ngyennhi/expensemanager/data/FirebaseAuthRepositoryTest.kt` |
| D | `app/src/test/java/com/ngyennhi/expensemanager/domain/usecase/ValidateAuthCredentialsUseCaseTest.kt` |
| D | `app/src/test/java/com/ngyennhi/expensemanager/presentation/auth/AuthViewModelTest.kt` |
| M | `build.gradle.kts` |
| M | `docs/ARCHITECTURE.md` |
| M | `docs/DESIGN.md` |
| M | `docs/PROJECT.md` |
| D | `docs/screenshots/auth-login.png` |
| D | `docs/screenshots/auth-register.png` |
| M | `docs/screenshots/settings-en-dark.png` |
| M | `docs/screenshots/settings-vi-light.png` |
| M | `gradle/libs.versions.toml` |
| M | `tasks/ROADMAP.md` |
| M | `tasks/TASK-011.md` |
| M | `tasks/TASK-012.md` |
| A | `tasks/TASK-013.md` |
