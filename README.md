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

## Getting started

Requires Android Studio supporting **AGP 9.4.1**, **JDK 25**, **SDK 37**, Build Tools **36.0.0**, and an **API 26+** device. Gradle is provided by the wrapper.

1. Clone the repository and open it in Android Studio.
2. Set your Android SDK path in local.properties.
3. Sync Gradle, select `app`, and click **Run**. No service configuration or credentials are required.

## Build & test

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

For the offline launch regression on a connected emulator, install updates without clearing storage:

```powershell
.\gradlew.bat :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.ngyennhi.expensemanager.presentation.OfflineFlowTest com.ngyennhi.expensemanager.test/com.ngyennhi.expensemanager.HiltTestRunner
```

The regression uses isolated test storage. For an existing installation, keep the application ID and signing key, use `adb install -r`, and do not uninstall or clear app storage. Room stays at schema v2 with database `expense_manager.db`; budgets keep `monthly_budgets` and their existing keys. [TASK-013](tasks/TASK-013.md) records release checks and the manual emulator checklist.

On macOS/Linux, use `./gradlew`. APK: `app/build/outputs/apk/debug/app-debug.apk`.

Verified on Pixel_5 / API 35: Debug Build, 96 Unit Tests, Lint (0 errors, 16 warnings, 1 hint), and 11 selected offline device tests passed. An in-place upgrade retained all 4 existing transactions, 1 wallet and saved budget preferences; schema v2 and stored values matched before/after installation and cold launch. API 26 and release-signed upgrade verification remain manual. See TASK-013 for details.

## Documentation

[Project](docs/PROJECT.md) · [Design](docs/DESIGN.md) · [Roadmap](tasks/ROADMAP.md) · [Contributing](AGENTS.md) · [CI](.github/workflows/android-ci.yml)
