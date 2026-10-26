# Personal Expense Manager

An offline Android app that helps users track income, expenses, and monthly spending. Built with **Kotlin**, **Jetpack Compose**, and **Room** as an Android portfolio project.

Users can record daily transactions, set a monthly budget, and understand their spending through charts. No account or internet connection is needed for the core features.

## Screenshots

Captured from the running app on a Pixel 5 emulator (Android 15 / API 35), using English and sample data. The app starts with an empty database on a fresh install.

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

## Features

- **Transaction management:** add, view, edit, and delete income or expense records with an amount, category, date, and optional note.
- **Monthly overview:** see total income, total expenses, net income, and the five most recent transactions for the selected month.
- **Search and filters:** search notes or categories and combine transaction type, category, and an inclusive date range.
- **Monthly budget:** set, edit, or remove each month's spending limit from Home → Budget. Switch months in the sheet and see the saved limit, actual expenses, percentage, and exact remaining amount. Dashboard and budget progress update when transactions change. Reached/exceeded warnings appear in the sheet, Dashboard, and in-app notices; percentages can exceed 100% and remaining amounts can be negative.
- **Statistics:** view expense shares by category and compare income and expenses over six months.
- **Category shortcuts:** choose a suggested or previously used category to start a new transaction.
- **Offline storage:** keep transactions in Room and budget limits in local preferences.
- **Vietnamese and English:** localized labels, dates, and amounts. Vietnamese is the default; Android 13+ supports language selection through system app settings.

## Tech stack

| Area | Tools |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3, custom Compose charts |
| Architecture | MVVM, Clean Architecture, one `:app` module |
| State and async work | ViewModel, StateFlow, Kotlin Coroutines, Flow |
| Navigation | Navigation Compose |
| Local storage | Room for transactions, SharedPreferences for monthly budgets |
| Dependency injection | Hilt, KSP |
| Testing | JUnit, Coroutines Test, Compose UI tests, AndroidX Test |
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

Amounts are stored as positive `Long` values in whole VND. Domain rules validate records, calculate totals with overflow checks, and apply filters. Currency formatting belongs to the UI.

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

Tests cover transaction validation, monthly totals, budgets, filtering, statistics, ViewModel state, Room persistence, and key Compose flows. Local HTML reports are generated under `app/build/reports/`. [Android CI](.github/workflows/android-ci.yml) runs the build, unit tests, and lint for pull requests and pushes to `main` and `develop`; device tests run separately.

Monthly budget scope, acceptance criteria, and verification are recorded in [TASK-008](tasks/TASK-008.md).

For local development, enable the tracked Git hooks with `git config core.hooksPath .githooks`. See [repository guidance](AGENTS.md) and the [PR checklist](.github/pull_request_template.md) for the contribution workflow.
