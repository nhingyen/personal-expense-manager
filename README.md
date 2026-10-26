# Personal Expense Manager

An offline Android app that helps users track income, expenses, and monthly spending. Built with **Kotlin**, **Jetpack Compose**, and **Room** as an Android portfolio project.

Users can record daily transactions, set a monthly budget, and understand their spending through charts. No account or internet connection is needed for the core features.

## Screenshots

Captured from the running app on a Pixel 5 emulator (Android 15 / API 35), using English and sample data. A fresh install creates the default cash wallet with zero opening balance and no sample transactions.

Home and Monthly budget were recaptured for TASK-008. The budget sheet shows month selection, the saved limit, spending progress, and the remaining amount.

| Home | Transactions | Add transaction | Transaction details |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/home.png" width="220" alt="Home with monthly balance, income, expenses, and budget progress"> | <img src="docs/screenshots/transactions.png" width="220" alt="Transaction list with search and income or expense filters"> | <img src="docs/screenshots/add-transaction.png" width="220" alt="Add transaction form with amount, category, date, and note"> | <img src="docs/screenshots/transaction-details.png" width="220" alt="Transaction details with edit and delete actions"> |

| Statistics | Transaction filters | Monthly budget | Categories |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/statistics.png" width="220" alt="Spending by category and six-month income and expense comparison"> | <img src="docs/screenshots/transaction-filters.png" width="220" alt="Transaction filters for category and date range"> | <img src="docs/screenshots/monthly-budget.png" width="220" alt="Monthly budget with month selection, saved limit, spent and remaining amounts, progress, and editing controls"> | <img src="docs/screenshots/categories.png" width="220" alt="Category shortcuts for adding income and expenses"> |

Additional TASK-008 budget states, captured from the running app:

| No budget or expenses | Over budget |
| :---: | :---: |
| <img src="docs/screenshots/monthly-budget-unset.png" width="220" alt="November budget with no saved limit, zero expenses, and an empty limit field"> | <img src="docs/screenshots/monthly-budget-exceeded.png" width="220" alt="October budget showing 136 percent used, negative remaining amount, and a warning that spending exceeds the limit by 920,000 VND"> |

The October example has 3,420,000 VND in expenses and a 5,000,000 VND limit. The over-budget capture changes that limit to 2,500,000 VND; November has no limit or expenses. Screenshots use fictional records in a temporary Android user, with no production seed data or image retouching.

TASK-009 wallet screens, captured from the running app with an isolated in-memory Room test database:

| Wallets | Wallet details | Edit wallet | Transaction wallet selection |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/wallets.png" width="220" alt="Cash, bank and e-wallet balances with an exact total"> | <img src="docs/screenshots/wallet-details.png" width="220" alt="Bank wallet balance, opening balance, protected deletion and its transactions"> | <img src="docs/screenshots/edit-wallet.png" width="220" alt="Wallet name, type and opening balance editor"> | <img src="docs/screenshots/transaction-wallet.png" width="220" alt="Add transaction with the bank wallet selected"> |

The wallet example has opening balances of 2,000,000 VND cash, 5,000,000 VND bank and 500,000 VND e-wallet, plus an 8,000,000 VND bank income and an 85,000 VND cash expense. The resulting total is 15,415,000 VND. Fixtures are test-only; no production sample records are installed. The earlier transaction gallery predates wallet selection; the new capture above shows the current form.

TASK-010 Settings, captured from the running app on the same API 35 emulator:

| Vietnamese / Light | English / Dark |
| :---: | :---: |
| <img src="docs/screenshots/settings-vi-light.png" width="220" alt="Vietnamese Settings with language, appearance, wallets and About"> | <img src="docs/screenshots/settings-en-dark.png" width="220" alt="English Settings with dark appearance and the installed app version"> |

The Settings captures use an isolated test database and the actual app UI, without image retouching. The earlier gallery predates dark appearance.

## Features

- **Transaction management:** add, view, edit, and delete income or expense records with an amount, category, date, optional note, and saved wallet. Change the wallet when editing a record.
- **Wallet management:** open **Settings → Wallets & accounts** to create or edit cash, bank, and e-wallets tracked manually. View each wallet's all-time transactions and exact balance: opening balance + income − expenses. Add from wallet details prefills that wallet. Signed opening balances are supported and do not count toward income, statistics, or budgets.
- **Safe wallet storage:** existing transactions migrate to the permanent default cash wallet with a zero opening balance; ordinary Add uses it by default. Only empty, non-default wallets can be deleted, after confirmation. Reassign or delete a wallet's transactions individually before deleting it.
- **Monthly overview:** see total income, total expenses, net income, and the five most recent transactions for the selected month.
- **Search and filters:** search notes or categories and combine transaction type, category, and an inclusive date range.
- **Monthly budget:** set, edit, or remove each month's spending limit from Home → Budget. Switch months in the sheet and see the saved limit, actual expenses, percentage, and exact remaining amount. Dashboard and budget progress update when transactions change. Reached/exceeded warnings appear in the sheet, Dashboard, and in-app notices; percentages can exceed 100% and remaining amounts can be negative.
- **Statistics:** view expense shares by category and compare income and expenses over six months.
- **Category shortcuts:** choose a suggested or previously used category to start a new transaction.
- **Offline storage:** keep wallets and transactions in Room, with a schema migration that preserves existing records; budget limits remain in local preferences. No transfers or bank connections are implemented.
- **Language:** Settings offers **System Default, Vietnamese and English** on API 26+. Labels, dates and amounts follow the selected language; saved categories/notes stay as entered. Vietnamese remains the fallback resource locale. Android 13+ system per-app language settings use the same preference.
- **Appearance and About:** choose **Light, Dark or System** for the whole app. Theme and language preferences persist locally. Settings also shows the offline app description and installed version, and keeps the Wallets entry.

## Tech stack

| Area | Tools |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3, custom Compose charts |
| Architecture | MVVM, Clean Architecture, one `:app` module |
| State and async work | ViewModel, StateFlow, Kotlin Coroutines, Flow |
| Navigation | Navigation Compose |
| Local storage | Room v2 for wallets and transactions, SharedPreferences for monthly budgets/theme, AndroidX/framework per-app language storage |
| Dependency injection | Hilt, KSP |
| Testing | JUnit, Coroutines Test, Compose UI tests, AndroidX Test, Room MigrationTestHelper |
| Automation | GitHub Actions: debug build, unit tests, and Android Lint |

## Architecture

The app separates UI, business rules, and storage inside a single module. Presentation and data both depend on domain; domain is pure Kotlin and has no Android dependencies.

```text
app/src/main/java/com/ngyennhi/expensemanager/
├── presentation/   Compose screens, ViewModels, UI state, navigation
├── domain/         Models, repository interfaces, use cases
├── data/           Room database, mappers, repository implementations
├── di/             Hilt modules and application setup
└── ui/theme/       Shared colors, typography, and Material theme
```

Room exposes transaction changes as a `Flow`. Repositories map stored records to domain models, ViewModels publish `StateFlow`, and Compose observes state with lifecycle awareness. User actions go back through ViewModels and use cases to update local storage.

Transaction amounts are positive `Long` values in whole VND; wallet opening balances are signed `Long` values. Each transaction references one wallet. Domain rules validate records, calculate totals with overflow checks, and apply filters. Wallet balance calculation uses exact intermediate arithmetic; opening balances never create transaction rows. Currency formatting belongs to the UI.

Read the [architecture guide](docs/ARCHITECTURE.md) for dependency rules and state handling.

## Getting started

### Requirements

- Android Studio with support for Android Gradle Plugin **9.4.1**.
- Android SDK Platform **37** (`platforms;android-37.0`) and Build Tools **36.0.0**, as used in CI.
- **JDK 25** for the Gradle daemon. The repository includes a Gradle **9.6.0** wrapper and daemon JVM configuration.
- An emulator or Android device running **Android 8.0 / API 26** or newer.

### Run the app

1. Clone the repository:

   ```bash
   git clone https://github.com/nhingyen/personal-expense-manager.git
   cd personal-expense-manager
   ```

2. Open the project in Android Studio and let Gradle sync finish. Set the local SDK path through Android Studio or an untracked `local.properties` file with `sdk.dir`.
3. Select the `app` run configuration and an emulator or connected device, then click **Run**.
4. Use **Add** to create a transaction. Set a spending limit from **Home → Budget** and open **Statistics** to see the recorded data.

Open **Settings → Wallets & accounts → Add wallet**, enter a name, choose Cash/Bank/E-wallet and enter a whole-VND opening balance. Tap a wallet to view its balance and all transactions, edit it or add a transaction to it. The transaction form also lets you choose a wallet. The default wallet is kept permanently and may be renamed. Wallet and transaction forms retain drafts through Activity recreation; unsaved drafts are not restored after process death.

In **Settings**, select **System Default / Vietnamese / English** and **Light / Dark / System**. Changes apply immediately and remain selected after relaunch. System Default clears the app language override; System appearance follows device dark mode. Language/theme changes preserve navigation, selected months, filters and activity ViewModel drafts, and do not modify saved transactions, wallets or budgets. About displays the installed app version. Cloud sync, authentication, backup/export, currency selection and delete-all are not implemented.

In **Budget**, use the month arrows to select a month, enter a positive whole-VND limit, and tap **Save budget**. Open it again to change or remove that month's limit. Month changes discard unsaved edits; Cancel closes the editor without saving. Only expense transactions in the selected month consume the budget. An unset limit and a month with no expenses have separate states. Budgets are local and independent per month; they do not roll over automatically.

To build a debug APK from PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. The first build needs internet access to download build tools and dependencies.

## Testing

Run the debug build, local unit tests, and lint:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

With an emulator or device connected, run instrumented tests:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

On macOS or Linux, replace `.\gradlew.bat` with `./gradlew` (or `bash ./gradlew`).

Tests cover transaction/wallet validation, balance isolation and overflow, monthly totals, budgets, filtering, statistics, ViewModel state, Room persistence, schema migration and key Compose flows. Migration tests use the exported v1/v2 schemas, check populated/empty databases, preserve transaction IDs and deleted-ID sequences, and verify foreign-key/delete guards. Local HTML reports are generated under `app/build/reports/`. [Android CI](.github/workflows/android-ci.yml) runs the build, unit tests, and lint for pull requests and pushes to `main` and `develop`; device tests run separately.

Monthly budget scope, acceptance criteria, and verification are recorded in [TASK-008](tasks/TASK-008.md).

Wallet scope, acceptance criteria, and verification are recorded in [TASK-009](tasks/TASK-009.md).

Settings/language/theme scope, acceptance criteria and actual verification are recorded in [TASK-010](tasks/TASK-010.md).

TASK-010 verification on Pixel_5/API 35 passed 27 device tests: nine Settings/language/theme tests plus 16 Home and two Wallet regression tests, run by class. Coverage includes selection changes, external Android language settings, configuration recreation, active transaction drafts/date dialogs, persisted theme XML, 200% text and unchanged Room records. The 96 local unit tests passed. API 26 runtime verification remains open because the emulator crashed in ANGLE/D3D11 before app startup; this is not a passed minimum-SDK check. See TASK-010 for commands and limits.

For local development, enable the tracked Git hooks with `git config core.hooksPath .githooks`. See [repository guidance](AGENTS.md) and the [PR checklist](.github/pull_request_template.md) for the contribution workflow.
