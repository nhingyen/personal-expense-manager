# Personal Expense Manager

An Android portfolio app for tracking income, expenses, wallets and monthly budgets. Core features work offline without an account.

## Screenshots

Captured on Pixel 5 / API 35. Financial screens use sample data; Settings screenshots predate the account card.

| Log in | Register |
| :---: | :---: |
| <img src="docs/screenshots/auth-login.png" width="220" alt="Login with a pastel welcome card, email/password fields and Guest mode"> | <img src="docs/screenshots/auth-register.png" width="220" alt="Registration with selected account tab and password confirmation"> |

| Home | Transactions | Wallets |
| :---: | :---: | :---: |
| <img src="docs/screenshots/home.png" width="220" alt="Monthly overview"> | <img src="docs/screenshots/transactions.png" width="220" alt="Transaction list"> | <img src="docs/screenshots/wallets.png" width="220" alt="Wallet balances"> |

| Statistics | Monthly budget | Settings |
| :---: | :---: | :---: |
| <img src="docs/screenshots/statistics.png" width="220" alt="Spending charts"> | <img src="docs/screenshots/monthly-budget.png" width="220" alt="Monthly spending limit"> | <img src="docs/screenshots/settings-en-dark.png" width="220" alt="Settings in dark mode"> |

## Features

- Add, edit and delete income/expenses with category, date, note and wallet.
- Manage manual cash, bank and e-wallet balances in VND.
- View monthly totals, budgets and spending warnings.
- Search and filter transactions by type, category and date.
- View category charts and six-month income/expense comparisons.
- Choose Vietnamese/English and Light/Dark/System appearance.
- Use Guest mode or optional Firebase Email/Password login, registration and logout.

**Data stays on the device and is shared by Guest mode and all accounts. It is not protected separately per account.** Login/logout never merges, deletes or syncs financial data. Cloud sync, Google Sign-In and bank connections are not implemented.

## Tech stack

Kotlin · Jetpack Compose · Material 3 · Room · Firebase Authentication · Hilt · Coroutines/StateFlow · Navigation Compose.

MVVM + Clean Architecture in one `:app` module: `presentation → domain ← data`. See [Architecture](docs/ARCHITECTURE.md).

## Getting started

Requires Android Studio supporting **AGP 9.4.1**, **JDK 25**, **SDK 37**, Build Tools **36.0.0**, and an **API 26+** device. Gradle is provided by the wrapper.

1. Clone the repository and open it in Android Studio.
2. Configure Firebase below and set your Android SDK path.
3. Sync Gradle, select `app`, and click **Run**.

### Firebase

For real accounts, register `com.ngyennhi.expensemanager` in Firebase, enable **Authentication → Email/Password**, and place the downloaded config at `app/google-services.json` (Git-ignored).

For Guest-only builds, if no local config exists:

```powershell
Copy-Item app/google-services.example.json app/google-services.json
```

The example uses dummy credentials; live authentication requires a real Firebase config. CI uses the example unless the optional **`FIREBASE_GOOGLE_SERVICES_JSON`** repository secret supplies the full JSON.

## Build & test

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Device tests, with an emulator/device connected:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

On macOS/Linux, use `./gradlew`. APK: `app/build/outputs/apk/debug/app-debug.apk`.

Latest verification: **112 unit tests**, **10 API 35 device tests**, and lint with **0 errors**. Authentication tests use fakes; live Firebase and API 26 runtime checks remain open. See [TASK-011](tasks/TASK-011.md) for details.

## Documentation

[Project](docs/PROJECT.md) · [Design](docs/DESIGN.md) · [Roadmap](tasks/ROADMAP.md) · [Contributing](AGENTS.md) · [CI](.github/workflows/android-ci.yml)
