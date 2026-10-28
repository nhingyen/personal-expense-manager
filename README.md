# Personal Expense Manager

**v1.0.0 - Offline-only.** An Android portfolio app for tracking income, expenses, wallets and monthly budgets. The app opens directly into Home with one local financial profile.

## Screenshots

Captured on Pixel 5 / API 35. Financial screens use sample data. Settings captures are refreshed for the offline release; earlier financial captures retain the same features.

| Home | Transactions |  |  |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/home.png" width="220" alt="Monthly overview"> | <img src="docs/screenshots/transactions.png" width="220" alt="Transaction list"> |  |  |

| Add transaction | Transaction details | Statistics | Transaction filters |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/add-transaction.png" width="220" alt="Add transaction form"> | <img src="docs/screenshots/transaction-details.png" width="220" alt="Transaction details"> | <img src="docs/screenshots/statistics.png" width="220" alt="Spending charts"> | <img src="docs/screenshots/transaction-filters.png" width="220" alt="Transaction filters"> |

| Monthly budget | Categories | No budget or expenses | Over budget |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/monthly-budget.png" width="220" alt="Monthly spending limit"> | <img src="docs/screenshots/categories.png" width="220" alt="Categories"> | <img src="docs/screenshots/monthly-budget-unset.png" width="220" alt="Monthly budget with no limit or expenses"> | <img src="docs/screenshots/monthly-budget-exceeded.png" width="220" alt="Monthly budget exceeded"> |

| Wallets | Wallet details | Edit wallet | Transaction wallet selection |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/wallets.png" width="220" alt="Wallet balances"> | <img src="docs/screenshots/wallet-details.png" width="220" alt="Wallet details"> | <img src="docs/screenshots/edit-wallet.png" width="220" alt="Edit wallet form"> | <img src="docs/screenshots/transaction-wallet.png" width="220" alt="Choose a wallet for a transaction"> |

| Vietnamese / Light | English / Dark |  |  |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/settings-vi-light.png" width="220" alt="Settings in Vietnamese and light mode"> | <img src="docs/screenshots/settings-en-dark.png" width="220" alt="Settings in English and dark mode"> |  |  |

## Features

- Add, edit and delete income/expenses with category, date, note and wallet.
- Manage manual cash, bank and e-wallet balances in VND.
- View monthly totals, budgets and spending warnings.
- Search and filter transactions by type, category and date.
- View category charts and six-month income/expense comparisons.
- Choose Vietnamese/English and Light/Dark/System appearance.
- Open and use every financial feature without an account or network.

Financial data remains in the existing local Room database and monthly budget preferences. Updating the app preserves that data; uninstalling or clearing Android app storage can remove it. This release has no login, per-account data ownership, in-app cloud backup or synchronization.

Android backup and device-transfer exports are disabled. Keep the installed app and its local storage when updating.

## Future Improvements

- Cloud Backup & Restore, with explicit ownership, validation and confirmed restoration.
- Local Multi-account, with separate financial data per profile and a deliberate plan for existing ownerless data.

## Tech stack

Kotlin · Jetpack Compose · Material 3 · Room · Hilt · Coroutines/StateFlow · Navigation Compose.

MVVM + Clean Architecture in one `:app` module: `presentation → domain ← data`. See [Architecture](docs/ARCHITECTURE.md).

## Launcher icon

The purple wallet logo from Image Asset Studio is configured for adaptive icons
(API 26+) and legacy square/round icons at all five standard densities. On
2026-10-28, Debug and unsigned Release builds passed with the new resources; the
installed Debug icon was visually verified in the Pixel 5 / API 35 App Drawer.
The signed v2 → v3 upgrade also verified the new icon on Home Screen and App
Drawer on the dedicated API 35 AVD. Other launcher masks and themed icons still
need manual review. [TASK-015](tasks/TASK-015.md) records the resource inventory;
the [signed-upgrade report](docs/SIGNED_UPGRADE_v2_v3.md) records the signed v3
artifact and actual icon replacement after updating.

## Getting started

Requires Android Studio supporting **AGP 9.4.1**, **JDK 25**, **SDK 37**, Build Tools **36.0.0**, and an **API 26+** device. Gradle is provided by the wrapper.

1. Clone the repository and open it in Android Studio.
2. Set your Android SDK path in local.properties.
3. Sync Gradle, select `app`, and click **Run**. No service configuration or credentials are required.

## Build & test

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest :app:lintDebug
```

For the offline launch regression on a connected emulator, install updates without clearing storage:

```powershell
.\gradlew.bat :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.ngyennhi.expensemanager.presentation.OfflineFlowTest com.ngyennhi.expensemanager.test/com.ngyennhi.expensemanager.HiltTestRunner
```

The regression uses isolated test storage. For an existing installation, keep the application ID and signing key, use `adb install -r`, and do not uninstall or clear app storage. Room stays at schema v2 with database `expense_manager.db`; budgets keep `monthly_budgets` and their existing keys. [TASK-013](tasks/TASK-013.md) records release checks and the manual emulator checklist.

On macOS/Linux, use `sh ./gradlew`. Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

Automatic Gradle release signing is **not configured**. `assembleRelease` produces
`app/build/outputs/apk/release/app-release-unsigned.apk`; a successful build does
not make that APK installable or ready for distribution. Use the intended release
key and verify an in-place signed upgrade before publishing. Android Studio can
sign an APK separately. The signed versionCode 2 APK has been verified and
archived read-only under `app/release/upgrade-verification/v2/` (ignored by Git).
Current source is versionCode **3**, versionName **1.0.0**; its unsigned Release
build passed with the new launcher icon. The supplied signed v3 APK verifies
with the same Release certificate; its dedicated API 35 in-place upgrade passed. The
[v2 → v3 runbook](docs/SIGNED_UPGRADE_v2_v3.md) uses a dedicated AVD and data
snapshots; never run instrumentation on the data-retention AVD. Verification
does not require sharing a keystore or signing passwords.

The signed-upgrade execution confirmed v2 still matched the saved baseline before
installing v3 with `adb install -r`. Comparisons immediately after install and
after cold launch both passed: three wallets, three transactions, all Room
IDs/relationships/schema/sequences/balances and budget/theme/locale retained.
UI values and the new icon on Home Screen/App Drawer passed. The AVD now retains
v3 and its sample data. The snapshot tool's seven host checks were run during
preparation; earlier app test counts below are historical.

Enable the tracked local hooks once per clone with
`git config core.hooksPath .githooks`. Pre-commit checks staged whitespace and unit
tests; pre-push runs Debug build and lint. CI runs Debug and unsigned Release
builds, unit tests and lint on PRs and pushes to `main`/`develop`; a separate API
26 emulator job runs the startup and Room migration instrumentation tests. These
new workflow gates are configured locally but have no remote result until the
workflow is pushed and runs. [Remote CI run 40480894011](https://github.com/nhingyen/personal-expense-manager/actions/runs/40480894011)
passed at HEAD `90d39062` using the earlier Debug/unit/lint-only workflow.

The current final audit is tracked in [TASK-014](tasks/TASK-014.md) and
[the release audit report](docs/RELEASE_AUDIT_v1.0.0.md). Historical TASK-013 results
are kept separately; the audit report records current-source results and remaining
release gates. The signed-upgrade gate passed on API 35; API 26 runtime and the
remaining CI/manual checks are still required before distribution.

Earlier app checks recorded in TASK-014: Debug/unsigned Release build passed; a fresh unit execution passed
98 tests; lint has 0 errors, 14 warnings and 1 hint. After adjusting the System
Theme test to avoid PixelCopy during a system night-mode change, the full API 35
instrumentation suite passed **63/63 continuously in 527.54 seconds**. An earlier
full run also passed 63/63 in 245.229 seconds; prior PixelCopy and ANR interruptions
remain documented. The ANR's root cause is still unknown. Room/budget/appearance
snapshots matched after the current run (SHA-256
`25d01cc3b8ac7857270f7665e93aa61d1fb2836f83546132a83292eecf6c070e`) and through
the earlier verification and offline Debug cold launch.
Additional API 26 AVD attempts with WHPX and varied encryption/data-partition
settings still did not complete boot; no app install or migration test ran there.
The new CI job targets API 26 for an isolated startup/migration run but remains
unverified remotely.
The audit remains **Not Ready for
distribution**: API 26 runtime/upgrade, remote CI and the remaining manual checks
are outstanding. Signed v3 verification/upgrade passed on API 35. No keystore/password changes,
commit/push or release publication were performed.

## Documentation

[Project](docs/PROJECT.md) · [Design](docs/DESIGN.md) · [Roadmap](tasks/ROADMAP.md) · [Contributing](AGENTS.md) · [CI](.github/workflows/android-ci.yml)
