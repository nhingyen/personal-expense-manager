# TASK-001: Architecture, dependencies, and localization

**Status:** In progress — API 26 startup verification remains

**Outcome:** Prepare the single-module Android app for later features with clear MVVM/Clean Architecture boundaries, the required dependency tooling, and English/Vietnamese resources. Keep the starter app launchable; transaction behavior and feature screens belong to later tasks.

## Starting point

- Keep `:app` as the only module and retain the existing AGP 9.4.1, Kotlin 2.2.10, Gradle 9.6.0, SDK levels, Compose, and Material 3 configuration where compatible. Change an existing version only if a required dependency demonstrably cannot work with it, and record why.
- Add all new plugin and library versions and aliases in `gradle/libs.versions.toml`. Apply plugins through those aliases; do not scatter version literals across build files.

## Architecture rules

Use packages under `com.ngyennhi.expensemanager` within `:app`:

| Package | Owns | Allowed dependencies |
| --- | --- | --- |
| `domain` | Pure Kotlin models, repository contracts, and use cases as later tasks introduce them | Kotlin and coroutine `Flow`/suspend APIs; no Android, Compose, Room, Hilt, `data`, or `presentation` types |
| `data` | Future local storage, mappers, and repository implementations | `domain`; Android/Room only inside this boundary |
| `presentation` | Compose UI, ViewModels, state, and events | `domain`; no DAO, Room entity, or direct database access |
| `di` | Hilt application wiring and bindings | Connects `data` implementations to `domain` contracts at the app boundary |

Keep the dependency direction `presentation -> domain <- data`, with `di` wiring the edges. ViewModels will expose immutable `StateFlow` state and call domain use cases; Compose will render that state and send events. Data will map storage types to domain types before returning them. Document these rules in `docs/ARCHITECTURE.md` if implementation reveals any needed clarification. The existing `ui.theme` starter package may stay in place until the first feature; do not add empty placeholder classes just to create packages.

## Dependency and app setup

1. Add compatible version-catalog aliases for the Hilt and KSP plugins and apply them to `:app` as required. Configure Hilt's application entry point and only the minimal startup wiring needed to prove initialization.
2. Add Room runtime, Room KTX, and the Room compiler through KSP. Configure the toolchain so later Room code can be generated; database entities, DAOs, migrations, and CRUD are TASK-002 work.
3. Add Hilt runtime and its KSP compiler, plus Kotlin coroutines/Flow dependencies needed by the app. Add lifecycle integration only where the current code needs it; avoid feature-specific libraries before a feature uses them.
4. Keep Compose and Material 3 in the existing app module. Do not add a second module or change dependency direction to make configuration easier.

## Localization rules

1. Make Vietnamese the unqualified fallback locale: keep user-facing strings in `app/src/main/res/values/strings.xml` and set `unqualifiedResLocale=vi` in `app/src/main/res/resources.properties`. Put corresponding English strings in `values-en/strings.xml`.
2. Move the starter screen's visible hardcoded greeting into string resources, including any interpolated text, and localize the app label. Future user-facing text must use Android resources with matching keys and valid placeholders/plurals in both languages. Keep locale-specific formatting at the presentation boundary; domain values remain locale-neutral.
3. Enable `androidResources.generateLocaleConfig` in `:app` for Android 13+ system per-app language settings. Ensure the generated supported-language list contains only `vi` and `en`, including when library translations are present (use Gradle resource configuration filtering where needed). Let AGP generate the manifest entry and locale config; do not add a manual `locale_config.xml` or manifest `android:localeConfig` entry. See [Android's per-app language guidance](https://developer.android.com/guide/topics/resources/app-languages).
4. The system language selects resources on older Android versions. Defer any in-app language picker, explicit locale storage, and pre-Android-13 picker behavior to a future Settings task.

## Verification and acceptance criteria

- `settings.gradle.kts` still includes only `:app`; the current AGP/Kotlin/Gradle/SDK setup is preserved unless a documented compatibility issue requires a change. All new dependencies and plugins resolve through the version catalog, and Hilt/KSP configuration generates code without errors.
- Source and updated architecture documentation show the four package responsibilities and dependency direction. Review of production imports finds no Android, Compose, Room, Hilt, `data`, or `presentation` types in `domain`, and no Room/DAO access in `presentation`.
- Vietnamese is the complete default resource set and English has matching user-visible strings. With an unsupported device language the starter UI falls back to Vietnamese; with English selected it shows English. No visible starter text is hardcoded in Kotlin.
- On an Android 13+ device or emulator, the app appears under system per-app language settings with only Vietnamese and English; switching either language updates its visible text and survives relaunch. Inspect the generated/merged locale configuration as part of this check.
- `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug` pass using the Gradle wrapper. Record the exact commands and results. Launch the debug app on API 26 and API 33+ devices or emulators and record the startup and language checks; do not mark this task complete while either startup check remains unverified.
- No transaction model, DAO, CRUD operation, feature screen, or in-app language picker is introduced.

Add focused tests only for meaningful behavior introduced here; use resource/lint inspection and device checks for locale and startup behavior. Update this task's scope and acceptance criteria if implementation changes them.

## Verification record (2026-10-19)

- Ran `.\gradlew.bat --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` after the final Gradle change: **passed**. The unit test run contains the existing starter test (1 passed); lint passed with 12 warnings and no errors.
- KSP and Hilt tasks completed. Generated sources include `Hilt_ExpenseManagerApplication`, `Hilt_MainActivity`, and `DaggerExpenseManagerApplication_HiltComponents_SingletonC`.
- Inspected the generated locale config and merged manifest: default locale is `vi`, the only advertised locales are `vi` and `en`, and the manifest references AGP's generated config. The Vietnamese and English string resource keys match.
- The installed API 35 `Pixel_5` emulator booted with host graphics. Installed the debug APK and launched `MainActivity`; its process remained alive and the Compose greeting was visible. KSP-generated Hilt application/activity entry points therefore also passed this startup check. Two earlier software-graphics emulator boot attempts stalled before APK installation.
- Android system app-language settings displayed English and Tiếng Việt. `cmd locale set-app-locales` changed the visible greeting to English and Vietnamese; the English choice persisted after force-stop and relaunch. With the per-app locale cleared and the effective system locale set to unsupported French, the visible greeting fell back to Vietnamese. The emulator's original English system locale was restored afterward.
- This SDK has no API 26 system image, so minimum-SDK startup remains unverified.

**Remaining device check:** Launch the debug APK on an API 26 device or emulator and confirm there is no startup crash before marking TASK-001 complete.

## Dependency maintenance: Firebase Kotlin compatibility (2026-10-26)

Scope: fix the build failure in the existing Firebase setup while retaining Kotlin 2.2.10 and the current AGP, Compose, KSP and Hilt tooling. Authentication screens and account behavior remain deferred.

The original `:app:compileDebugKotlin` run failed because Firebase BoM 35.1.0 selected `firebase-auth:25.0.0`, whose Kotlin metadata version is 2.4.0; the project compiler expected 2.2.0. Pin the catalog's BoM to 34.10.0, which selects Auth 24.0.1 according to the [Firebase release mapping](https://firebase.google.com/support/release-notes/android#bom_v34-10-0). Keep the Auth dependency versionless under the BoM and keep compiler metadata checks enabled.

Acceptance criteria:

- Gradle dependency insight selects Auth 24.0.1 from BoM 34.10.0.
- The debug APK, existing local unit tests and debug lint pass with Kotlin 2.2.10.
- README records the compatibility pin and actual verification; no authentication feature is claimed.

Verification with the final BoM 34.10.0 configuration:

- `.\gradlew.bat :app:dependencyInsight --dependency com.google.firebase:firebase-auth --configuration debugCompileClasspath --console=plain`: **passed**; Auth 24.0.1 is selected by the BoM 34.10.0 constraint.
- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain`: **passed** in 3m 25s. Kotlin compilation completed without metadata errors; the debug APK was generated at `app/build/outputs/apk/debug/app-debug.apk`.
- Unit-test XML reports: **96 tests, 0 failures, 0 errors, 0 skipped** across 17 suites. Lint XML reports: **0 errors, 15 warnings, 1 hint**.
- `git diff --check`: **passed**. AGP 9.4.1, Kotlin 2.2.10, Hilt 2.60.1 and KSP 2.3.12 remain unchanged; no metadata-check bypass was added.
- No device tests or authentication runtime checks were run for this dependency fix. The existing API 26 startup verification remains open. No commit or push was performed.
