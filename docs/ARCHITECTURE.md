# Architecture

## Single-module layout

Keep one Gradle module, `:app`, with packages under `com.ngyennhi.expensemanager`:

| Package | Responsibility |
| --- | --- |
| `domain` | Pure Kotlin models, repository interfaces, and business rules/use cases. |
| `data` | Room entities/DAO/database, mappers, and repository implementations. |
| `presentation` | Compose screens, ViewModels, UI state, and event handling. |
| `di` | Hilt bindings and application wiring. |

`di.ExpenseManagerApplication` is the Hilt application entry point. The starter `MainActivity` remains at the app root as the Android entry point and renders the `presentation.Greeting` composable. The existing `ui.theme` package can move under `presentation` when the first feature is built. `domain` and `data` gain source files when TASK-002 introduces their first responsibilities; empty placeholder classes are unnecessary.

## Dependency rules

`presentation -> domain <- data`; `di` connects implementations to domain interfaces at the app boundary. Domain must not import Android, Compose, Room, Hilt, or presentation/data classes. Data must not depend on presentation. Compose screens must not call DAOs or know Room entities. These package rules are enforced by review while the code remains in one module.

AGP 9 supplies Kotlin compilation for the Android module; only the Compose compiler, KSP, and Hilt plugins are applied separately. Their versions and all new library versions live in the Gradle version catalog. Room's runtime, KTX, and KSP compiler are configured for TASK-002; there is no database or DAO in TASK-001.

## Data and state flow

Room is the future local source of truth. A repository exposes domain models and `Flow` streams; use cases express business rules. ViewModels collect flows with lifecycle-aware coroutines, expose immutable `StateFlow<UiState>`, and handle user actions. Compose renders state and sends events to ViewModels. Use suspend functions for writes and explicit error handling for failures. Keep currency values as integer minor units and dates as a stable domain representation; mapping to storage and display formats stays at the boundaries.

## Localization

Vietnamese is the default resource locale (`values/strings.xml` and `res/resources.properties`); English translations live in `values-en/strings.xml`. User-facing text belongs in Android string/plural resources with matching keys and placeholders. Date, number, and currency display formatting belongs in presentation, while domain values remain locale-neutral.

AGP generates the locale configuration for Android 13+ system per-app language settings. The app filters packaged language resources to `vi` and `en`, so library translations cannot add unsupported languages to the generated list. The app does not provide its own language picker; that belongs to a future Settings task.

## Testing

Test domain rules with local unit tests; test repository mapping and observable updates when Room is introduced; add focused UI tests for important user flows. Each roadmap task names the checks that prove its acceptance criteria.
