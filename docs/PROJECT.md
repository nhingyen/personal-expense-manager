# Project brief

## Goal

Build an offline personal expense tracker that demonstrates clear Android fundamentals at Intern/Fresher level: readable Kotlin, reactive UI, persistence, tests, and a reviewable development process.

## MVP

- Record income and expenses with amount, category, date, and optional note.
- View, edit, and delete records; show a monthly income, expense, and balance summary.
- Persist records locally with Room so core flows work without an account or network.
- Handle empty, loading, and error states in a usable Material 3 Compose UI.

Money will be stored as a positive `Long` count of whole VND; display formatting belongs to presentation. The initial release has no sync, authentication, OS notification delivery, or currency conversion. TASK-004 additionally supports a local monthly spending limit and in-app budget notices.

## Technical direction

Kotlin, Jetpack Compose, Material 3, MVVM plus Clean Architecture, a single `:app` module, coroutines and Flow/StateFlow, Hilt for injection, and Room for local storage. Minimum SDK: 26. See [ARCHITECTURE.md](ARCHITECTURE.md) for dependency rules.

## Current state

TASK-014 audits the Offline-only release, fixes confirmed backup-policy and pending-transaction-write defects, and records current Gradle/device evidence. Automatic Gradle signing is not configured. Signed v2 was verified and archived; source is versionCode 3/name 1.0.0. The user-signed v3 verifies with the same Release certificate. The dedicated API 35 v2 → v3 upgrade passed preflight, install -r, both post-install/cold-launch Room/preferences/locale comparisons, UI totals and the TASK-015 icon on Home Screen/App Drawer. API 26 runtime, remote CI and remaining manual checks remain release gates. See the [final audit report](RELEASE_AUDIT_v1.0.0.md) and [signed-upgrade report](SIGNED_UPGRADE_v2_v3.md); historical task test counts are not current release certification.

The latest Final Release Verification continuation passed a fresh 98-test unit
execution and one uninterrupted 63-test API 35 instrumentation run. Existing
financial/appearance snapshots remained unchanged through tests and offline cold
launch. The adjusted PixelCopy test passed in a current-source full API 35 suite.
Additional API 26 AVDs with WHPX and varied encryption/data sizes did not complete
boot, so startup/migration remain unverified there. The distribution decision remains Not Ready, with no signing
key/password changes or release publication.

TASK-013 prepares Offline-only v1.0.0: the app opens directly into Home with one local financial profile. Firebase Authentication, Login/Register, Google Sign-In and account UI from TASK-011/TASK-012 are retired. All Transaction, Wallet, Budget, Dashboard, Statistics and Settings features remain local. Room schema v2 and the existing database/preferences names and keys are preserved for updates. Cloud Backup & Restore and Local Multi-account are Future Improvements. See [TASK-013](../tasks/TASK-013.md) for acceptance criteria, checks and remaining manual verification.

TASK-010 adds Settings language (System Default/VI/EN), Light/Dark/System appearance, persisted preferences and About/installed version. It retains the existing Wallets entry, shared bottom navigation and configuration state. See [TASK-010](../tasks/TASK-010.md) for verification; this supersedes earlier Settings/language/theme deferrals below. Cloud sync, authentication, backup and export remain outside scope.

TASK-009 adds manual cash/bank/e-wallet management from Settings, wallet balances and wallet-linked transactions. Room v2 migrates existing records into a permanent default cash wallet without changing report totals. Wallet detail lists all of its transactions; Add/edit can select a saved wallet. No bank integration or transfers. See [TASK-009](../tasks/TASK-009.md) for acceptance criteria and verification; it supersedes the Wallet/Settings-placeholder deferrals below.

TASK-008 extends the existing Home budget sheet with direct month selection, saved-limit/spent/remaining overview, month-specific edit/remove, and reached/exceeded warnings shared with Dashboard. It reuses the existing budget use cases/preferences and Room transactions; see [TASK-008](../tasks/TASK-008.md) for acceptance criteria and current verification.

The single app module has pure domain transaction, monthly-summary, monthly-budget and statistics rules, Room transaction persistence, Hilt wiring, and Compose Home/list/add/edit/detail/Statistics flows with ViewModel StateFlow and Navigation. Home shows a clock-based greeting, selected-month totals, Budget/Categories shortcuts, an editable persisted monthly spending limit with actual expense progress, budget notices, and up to five recent Room records. Categories reuse form suggestions and saved transaction categories to prefill Add. TASK-005 adds the Figma Home/Transactions/Add/Statistics/Settings bottom bar. TASK-007 replaces the Statistics placeholder with monthly income/expense, expense category distribution and a six-month income/expense comparison from Room; Settings implements TASK-010 language, appearance, Wallets and About controls. Add opens the existing form, and the bar is hidden on add/edit/detail. Tab navigation saves/restores screen state and keeps Home as the Back root; View all still explicitly selects Home's month. Vietnamese is the default locale and English resources are provided. Verification is recorded in TASK-004, TASK-005 and TASK-007; minimum-SDK startup verification on API 26 from TASK-001 remains open. The [roadmap](../tasks/ROADMAP.md) defines the order of work.
