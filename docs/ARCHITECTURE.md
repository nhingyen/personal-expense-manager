# Architecture

## Single-module layout

Keep one Gradle module, `:app`, with packages under `com.ngyennhi.expensemanager`:

| Package | Responsibility |
| --- | --- |
| `domain` | Pure Kotlin models, repository interfaces, and business rules/use cases. |
| `data` | Room entities/DAO/database, preference storage, mappers, and repository implementations. |
| `presentation` | Compose screens, ViewModels, UI state, and event handling. |
| `di` | Hilt bindings and application wiring. |

`di.ExpenseManagerApplication` is the Hilt application entry point. `MainActivity` remains at the app root and renders `presentation.ExpenseManagerApp`. TASK-002 provides the `domain` contract/use cases and `data` Room implementation; TASK-003 adds transaction screens and MVVM; TASK-004 integrates Dashboard and app navigation. The existing `ui.theme` package remains the shared Design System.

Transaction UI is organized by screen: `TransactionListScreen.kt` owns the list and empty helpers, `TransactionFormScreen.kt` owns the shared add/edit form and field helpers, and `TransactionDetailScreen.kt` owns detail and its field helper. `TransactionComponents.kt` shares the transaction row, type chip, category icon, error banner, and unavailable-record state. Feature-independent UI lives in `presentation.common.components`: `AppTopBar`, `MonthSelector`, and `PrimaryNavigationBar`. `ExpenseManagerApp.kt` owns navigation and lifecycle-aware collection of both activity-scoped ViewModels.

`MonthlySummaryCard.kt` owns Dashboard's localized financial display and text-measured two-column/full-width layout. It renders existing domain summary values without calculating totals. Shared bars follow the measured Figma geometry while retaining Android insets, scalable content, selected tab semantics, and a button action for Add. The shared transaction row promotes a saved note to its main title, uses category/date as supporting text, and exposes the localized type in accessibility state semantics.

## Dependency rules

`DashboardScreen` consumes `DashboardUiState` and emits callbacks; it does not observe repositories, inject a ViewModel, or own navigation. `DashboardViewModel` reuses `ObserveTransactionsUseCase` and the pure `CalculateMonthlySummaryUseCase`. Summary and the five recent rows derive from one snapshot, independently of transaction filters. Month changes use the cached snapshot without opening another collector; `SavedStateHandle` preserves the selected month. A failed observation retains clearly stale data; retry replaces the observation and respects cancellation. Exact `Long` arithmetic reports overflow instead of showing false totals.

Home is the navigation root. Home and Transactions show the shared three-entry bar; Add opens the existing form. Activity-scoped ViewModels preserve selections across tab/form/detail transitions. View all explicitly selects the Dashboard month and clears transaction query/type filters; ordinary tab switching preserves them. Successful writes and missing-record return actions navigate to Transactions after popping to Home, so a pre-existing list entry is unnecessary. Back from Transactions returns to Home; repeated tab switches cannot stack duplicate destinations. Detail/edit retain existing secondary back controls.

`presentation -> domain <- data`; `di` connects implementations to domain interfaces at the app boundary. Domain must not import Android, Compose, Room, Hilt, or presentation/data classes. Data must not depend on presentation. Compose screens must not call DAOs or know Room entities. These package rules are enforced by review while the code remains in one module.

AGP 9 supplies Kotlin compilation for the Android module; only the Compose compiler, KSP, and Hilt plugins are applied separately. Their versions and all new library versions live in the Gradle version catalog. Room's runtime, KTX, and KSP compiler generate the TASK-002 database and DAO; the version 1 schema is exported under `app/schemas`.

## Data and state flow

TASK-004's Home completion adds `MonthlyBudgetRepository` and observe/save/calculate budget use cases in domain. `PreferencesMonthlyBudgetRepository` persists one positive whole-VND limit per `YearMonth` in app-private SharedPreferences, exposes changes through a cancellable Flow, and performs checked writes on IO. Hilt wires storage separately from the pure use cases. This small preferences store does not change the Room transaction schema or introduce a dependency.

DashboardViewModel owns one transaction observer and one budget-limit observer. Budget use derives from the same selected-month expense total as the summary; summary, recent rows, and budget are published together on transaction updates. Percentage uses exact BigInteger arithmetic; only the visual progress bar is capped at 100%. Unset, loading, read/write error, overflow, and explicitly stale transaction data remain distinct. Saving is validated and blocks duplicate writes. Budget/category/notification sheets receive state and callbacks; they never access repositories. Categories reuse the transaction form's localized suggestions plus saved categories; selecting one prefills the existing Add flow. The bell shows actual selected-month budget notices without OS delivery or fabricated history.

Room is the local source of truth. `TransactionDao` exposes a Room `Flow`; `RoomTransactionRepository` maps entities to domain records and implements the domain repository contract. Pure domain use cases validate writes and expose the observation stream. `TransactionViewModel` collects that stream in `viewModelScope`, exposes immutable `StateFlow<TransactionUiState>`, and handles user actions; Compose collects state with lifecycle awareness, renders it, and sends events. Navigation Compose owns list, add, detail, and edit routes. Use suspend functions for writes and explicit error handling for failures. Keep amounts as positive `Long` values in whole VND and dates as `LocalDate` in domain; the mapper stores dates as epoch-day integers and presentation formats values for display.

## Localization

Vietnamese is the default resource locale (`values/strings.xml` and `res/resources.properties`); English translations live in `values-en/strings.xml`. User-facing text belongs in Android string/plural resources with matching keys and placeholders. Date, number, and currency display formatting belongs in presentation, while domain values remain locale-neutral.

AGP generates the locale configuration for Android 13+ system per-app language settings. The app filters packaged language resources to `vi` and `en`, so library translations cannot add unsupported languages to the generated list. The app does not provide its own language picker; that belongs to a future Settings task.

## Testing

Test domain rules with local unit tests; test repository mapping and observable updates when Room is introduced; add focused UI tests for important user flows. Each roadmap task names the checks that prove its acceptance criteria.
