# Architecture

## Single-module layout

Keep one Gradle module, `:app`, with packages under `com.ngyennhi.expensemanager`:

| Package | Responsibility |
| --- | --- |
| `domain` | Pure Kotlin models, repository interfaces, and business rules/use cases. |
| `data` | Room entities/DAO/database, mappers, and repository implementations. |
| `presentation` | Compose screens, ViewModels, UI state, and event handling. |
| `di` | Hilt bindings and application wiring. |

`di.ExpenseManagerApplication` is the Hilt application entry point. `MainActivity` remains at the app root and renders `presentation.transaction.TransactionApp`. TASK-002 provides the `domain` contract/use cases and `data` Room implementation; TASK-003 adds a transaction ViewModel, UI state, Navigation Compose routes, and list/form/detail composables under `presentation.transaction`. The existing `ui.theme` package remains the shared Design System.

Transaction UI is organized by screen: `TransactionListScreen.kt` owns the list and its row/empty helpers, `TransactionFormScreen.kt` owns the shared add/edit form and its field-error text/currency suffix helpers, and `TransactionDetailScreen.kt` owns detail and its field helper. `TransactionComponents.kt` contains UI shared across screens, including the top bar, type chip, category icon, error banner, and unavailable-record state. `TransactionApp.kt` owns navigation; screen extraction does not change ViewModel or domain responsibilities.

## Dependency rules

`presentation -> domain <- data`; `di` connects implementations to domain interfaces at the app boundary. Domain must not import Android, Compose, Room, Hilt, or presentation/data classes. Data must not depend on presentation. Compose screens must not call DAOs or know Room entities. These package rules are enforced by review while the code remains in one module.

AGP 9 supplies Kotlin compilation for the Android module; only the Compose compiler, KSP, and Hilt plugins are applied separately. Their versions and all new library versions live in the Gradle version catalog. Room's runtime, KTX, and KSP compiler generate the TASK-002 database and DAO; the version 1 schema is exported under `app/schemas`.

## Data and state flow

Room is the local source of truth. `TransactionDao` exposes a Room `Flow`; `RoomTransactionRepository` maps entities to domain records and implements the domain repository contract. Pure domain use cases validate writes and expose the observation stream. `TransactionViewModel` collects that stream in `viewModelScope`, exposes immutable `StateFlow<TransactionUiState>`, and handles user actions; Compose collects state with lifecycle awareness, renders it, and sends events. Navigation Compose owns list, add, detail, and edit routes. Use suspend functions for writes and explicit error handling for failures. Keep amounts as positive `Long` values in whole VND and dates as `LocalDate` in domain; the mapper stores dates as epoch-day integers and presentation formats values for display.

## Localization

Vietnamese is the default resource locale (`values/strings.xml` and `res/resources.properties`); English translations live in `values-en/strings.xml`. User-facing text belongs in Android string/plural resources with matching keys and placeholders. Date, number, and currency display formatting belongs in presentation, while domain values remain locale-neutral.

AGP generates the locale configuration for Android 13+ system per-app language settings. The app filters packaged language resources to `vi` and `en`, so library translations cannot add unsupported languages to the generated list. The app does not provide its own language picker; that belongs to a future Settings task.

## Testing

Test domain rules with local unit tests; test repository mapping and observable updates when Room is introduced; add focused UI tests for important user flows. Each roadmap task names the checks that prove its acceptance criteria.
