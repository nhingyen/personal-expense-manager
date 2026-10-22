# Personal Expense Manager

An Android Kotlin portfolio project for tracking income and expenses offline. The app now has a localized transaction list, add/edit form, and detail/delete flow backed by Room. Monthly summary and other screens are planned in the [roadmap](tasks/ROADMAP.md).

## Planned app

Record income and expenses, browse and edit them, and see a monthly balance. The implementation will use Compose, Material 3, MVVM plus Clean Architecture, Room, coroutines/Flow, and Hilt in one `:app` module. Minimum SDK is 26. See the [project brief](docs/PROJECT.md) and [architecture](docs/ARCHITECTURE.md).

## Build and test

Open the project in Android Studio with an Android SDK compatible with `compileSdk 37`. The Gradle wrapper uses Gradle 9.6.0, and `gradle/gradle-daemon-jvm.properties` requests JDK 25. From the repository root:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

On macOS/Linux, use `./gradlew` instead.

With an Android device or emulator connected, run the Room and Hilt tests:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

## Development process

Activate the tracked Git hooks once per clone:

```text
git config core.hooksPath .githooks
```

Pre-commit checks staged whitespace and runs unit tests. Pre-push builds and runs lint. Git for Windows uses `gradlew.bat`; Unix uses `gradlew`. Open a PR to `main` or `develop`, fill in the [PR template](.github/pull_request_template.md), and wait for [Android CI](.github/workflows/android-ci.yml) before merging. See [TASK-000](tasks/TASK-000.md) and [AGENTS.md](AGENTS.md) for the full process.
