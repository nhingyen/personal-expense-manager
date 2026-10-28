# Roadmap

Work in task order. Each task should produce one reviewable PR; update scope and acceptance criteria before implementation when requirements change. [TASK-000.md](TASK-000.md) expands the foundation task, and [TASK-002.md](TASK-002.md) defines the next storage task's observable checks.

| Task | Outcome | Acceptance criteria |
| --- | --- | --- |
| TASK-000 | Development foundation | Project and architecture docs agree; branch/PR/review process is documented; installable tracked Git hooks check staged whitespace; GitHub Actions runs lint, unit tests, and debug build for PRs and the default branch; a PR checklist records review and verification. |
| TASK-001 | Architecture, dependencies, and localization | One `:app` module remains with MVVM/Clean Architecture boundaries; Room, Hilt, KSP, and coroutine/Flow setup uses the version catalog; Vietnamese is the default with English resources and Android 13+ system per-app language support; build, unit tests, lint, and startup checks pass. |
| TASK-002 | Transaction Domain & Room Persistence | Domain model and repository contract cover income/expense records; Room stores positive `Long` amounts in whole VND and supports observe/create/update/delete; mapping and persistence tests pass, including an empty database and a reopened database. |
| TASK-003 | Transaction UI & MVVM Integration | Compose list and add/edit screens use Material 3 and ViewModels; users can create, edit, and delete records offline; validation prevents invalid amounts; UI state covers empty/loading/error; core flow is verified by focused tests or documented manual checks. |
| TASK-004 | Monthly Home + MVVM integration | Home follows the Figma greeting, bell, summary, Budget/Categories shortcuts, monthly-budget progress, and recent-record layout; spending limits persist locally and progress follows Room expenses; categories prefill Add and the bell opens actual budget notices. Bottom navigation is tracked separately in TASK-005. Month survives navigation/recreation; View all, floating/empty-state Add, Android Back, and direct detail/save/delete paths work; calculation, state, persistence, localization, and navigation checks are recorded in [TASK-004.md](TASK-004.md). |
| [TASK-005](TASK-005.md) | Bottom Navigation & App Navigation | Figma Home/Transactions/Add/Statistics/Settings bar reuses existing screens and Design System; Statistics/Settings are localized placeholders; destination selection, Back, save/restore state and hidden bar on secondary screens are tested; build, unit tests and lint are recorded. |
| [TASK-007](TASK-007.md) | Statistics & Charts | Statistics replaces its placeholder with Figma-based monthly totals, expense category donut/percentages and six-month income/expense comparison; existing Room repository/use cases feed MVVM + StateFlow; month restoration, reactive updates, empty/error/zero/overflow handling and VI/EN are covered by focused tests. Build, Unit Test and Lint results are recorded in the task. |
| [TASK-006](TASK-006.md) | Transaction Search & Filter | Note/category search, type/category/inclusive date-range filters, reset, reactive Room updates, independent Home totals, and VI/EN editor validation. Portfolio release verification is tracked in TASK-014. |
| [TASK-008](TASK-008.md) | Monthly Budget Management | Extend existing Home budgets with month selection, saved limit/expense/remaining overview, edit/remove, and reached/exceeded warnings. Reuse current domain, Room transaction observation, preference storage and MVVM StateFlow. Unset/zero/error/overflow, month/draft isolation, transaction updates and VI/EN are covered by focused tests; Build, Unit Test and Lint results are recorded in the task. README matches implemented and verified scope. |
| [TASK-009](TASK-009.md) | Wallet Management | Manual cash/bank/e-wallet list, create/edit/detail, exact balances and transaction wallet selection; Room v2 migration preserves old records in the default wallet; guarded deletion, VI/EN, focused tests and Build/Unit Test/Lint verification. |
| [TASK-010](TASK-010.md) | Settings & Language | Figma Settings with System Default/VI/EN on API 26+, Light/Dark/System appearance, persisted preferences, About/installed version and the existing Wallets entry. Preserve navigation, UI state and Room data when switching; focused unit/device tests and Build/Unit Test/Lint verification. |
| [TASK-011](TASK-011.md) (historical; retired by TASK-013) | Firebase Authentication & Guest Mode | Optional Email/Password login/register, restored auth state and Settings logout; Guest retains all offline features. Local financial data stays device-wide without account isolation, merging, deletion or sync. VI/EN validation/loading/errors, focused tests, Firebase-compatible CI and Build/Unit Test/Lint verification. |
| [TASK-012](TASK-012.md) (historical; retired by TASK-013) | Google Sign-In extension to TASK-011 | Credential Manager Google button and Firebase token exchange reuse the existing auth state. Explicit provider separation keeps same-email Email/Password accounts independent; cancellation, errors, tests and console setup are documented. |
| [TASK-013](TASK-013.md) | Offline-only v1.0.0 | Remove auth/cloud UI and Firebase configuration; retain every financial feature and Room v2/monthly budget data; verify Build, Unit Tests, Lint, offline device flows and an in-place update without data loss. |
| [TASK-014](TASK-014.md) | Final QA & Release Readiness Audit | Audit all offline features and release configuration; fix only reproduced bugs; run Debug/Release builds, unit tests, lint and available isolated device/migration tests; verify local data preservation; record P0/P1/P2 findings, signing status and outstanding manual release gates. |
| [TASK-015](TASK-015.md) | Launcher Icon verification | Verify Image Asset Studio adaptive/legacy resources, manifest references and packaged icons; pass Debug/Release builds; inspect the icon on an available emulator and document remaining manual checks without uninstalling or clearing app data. |

TASK-000 is documentation and development workflow only. App features and new dependencies begin in later tasks.
TASK-006 implements transaction search/filter; TASK-014 owns final release verification. TASK-005's Statistics placeholder is superseded by TASK-007. TASK-010 extends TASK-009's Settings/Wallets entry with language, theme and About. TASK-011/TASK-012 are historical authentication work retired by TASK-013; v1.0.0 has one local financial profile. Export, automatic cloud sync and delete-all remain deferred.

TASK-014's latest continuation has a fresh 98-test unit pass and an uninterrupted
63-test API 35 instrumentation pass; earlier failed/interrupted attempts remain
in the audit report. Additional API 26 AVDs did not complete boot, so API 26
startup/migration 1→2 and upgrade remain unverified. Distribution stays blocked on
API 26, remote CI and the documented manual gates; the later signed-upgrade
result below closes that gate on API 35. Keystore/password
changes, commit/push and release creation are excluded from this verification scope.

The open-issues review confirmed a passing remote Debug/unit/lint run at HEAD
90d39062, covering neither uncommitted TASK-014 changes nor Release/device gates.
The missing-artifact finding from that review is superseded by the later signed
v2/v3 verification; adding automatic signing configuration is not an acceptance criterion.

TASK-014 signed-upgrade continuation (2026-10-28) verified and archived the signed
Release versionCode 2 APK, then increased source versionCode to 3 with versionName
1.0.0 unchanged. Unsigned v3 Release build passed with the TASK-015 launcher icon.
The user supplied signed v3 with the same Release certificate. A fresh v2 snapshot
matched the original baseline before install; `adb install -r` and comparisons
after install/cold launch passed with unchanged Room/IDs/balances/preferences/
locale, and the new icon appeared on Home Screen/App Drawer. Signed upgrade is
PASS on API 35; API 26 and remote CI/manual release gates remain open. The
[runbook](../docs/SIGNED_UPGRADE_v2_v3.md) reserves a dedicated API 35 AVD with no
instrumentation tests, uninstall or data clearing.

TASK-008 was reassigned from the previous README/screenshots documentation work to Monthly Budget Management at the user's request. The existing gallery remains; the broader accessibility and portfolio release checks remain open. TASK-006.md records the earlier transaction search/filter implementation.

## Future Improvements

- **Cloud Backup & Restore:** explicit manual backup/confirmed restore, defined account ownership, data validation and security-rule tests before implementation. No current release implementation.
- **Local Multi-account:** isolate Guest and each UID across transactions, wallets, budget preferences and UI state; first define how existing ownerless data is retained or explicitly assigned. No automatic migration of existing data to a signed-in account.
