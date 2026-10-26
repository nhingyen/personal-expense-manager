# Architecture

## Single-module layout

Keep one Gradle module, `:app`, with packages under `com.ngyennhi.expensemanager`:

| Package | Responsibility |
| --- | --- |
| `domain` | Pure Kotlin models, repository interfaces, and business rules/use cases. |
| `data` | Room entities/DAO/database, preference storage, mappers, and repository implementations. |
| `presentation` | Compose screens, ViewModels, UI state, and event handling. |
| `di` | Hilt bindings and application wiring. |

`di.ExpenseManagerApplication` is the Hilt application entry point. `MainActivity` remains at the app root and renders `presentation.ExpenseManagerApp`. TASK-002 provides the `domain` contract/use cases and `data` Room implementation; TASK-003 adds transaction screens and MVVM; TASK-004 integrates Home and app navigation. The existing `ui.theme` package remains the shared Design System.

Transaction UI is organized by screen: `TransactionListScreen.kt` owns the list and empty helpers, `TransactionFormScreen.kt` owns the shared add/edit form and field helpers, and `TransactionDetailScreen.kt` owns detail and its field helper. `TransactionComponents.kt` shares the transaction row, type chip, category icon, error banner, and unavailable-record state. Feature-independent UI lives in `presentation.common.components`: `AppTopBar` and `MonthSelector`. `ExpenseManagerApp.kt` owns navigation and lifecycle-aware collection of both activity-scoped ViewModels.

`MonthlySummaryCard.kt` owns Home's localized financial display and text-measured two-column/full-width layout. It renders existing domain summary values without calculating totals. The shared top bar follows the measured Figma geometry while retaining Android insets and scalable content. In the app host, Home and Transactions use the centered bottom Add action and retain empty-state Add buttons. Their optional floating action remains available to standalone screen callers. The shared transaction row promotes a saved note to its main title, uses category/date as supporting text, and exposes the localized type in accessibility state semantics.

## Dependency rules

Home presentation lives in `presentation.home`. `HomeScreen` consumes `HomeUiState` and emits callbacks; it does not observe repositories, inject a ViewModel, or own navigation. `HomeViewModel` reuses `ObserveTransactionsUseCase` and the pure `CalculateMonthlySummaryUseCase`. Summary and the five recent rows derive from one snapshot, independently of transaction filters. Month changes use the cached snapshot without opening another collector; `SavedStateHandle` preserves the selected month using the existing storage key. A failed observation retains clearly stale data; retry replaces the observation and respects cancellation. Exact `Long` arithmetic reports overflow instead of showing false totals.

Home is the navigation root. TASK-005 adds `presentation.navigation` for centralized routes/destinations, top-level navigation policy, the shared bottom bar, and Statistics/Settings placeholders. `ExpenseManagerApp` owns one Navigation Compose controller and outer Material 3 Scaffold. The current destination determines selection and bar visibility; add/detail/edit have no bottom bar. The outer Scaffold consumes only its bottom padding so existing screen scaffolds keep status/IME insets without applying the bottom inset twice.

Ordinary tab switches use `popUpTo(Home) { saveState = true }`, `launchSingleTop` and `restoreState`; tapping the active tab is a no-op. Home stays the single Back root, so Back from another primary tab returns to Home rather than cycling through visited tabs. Navigation Compose retains saveable content (including list scroll) and restores the selected destination after Activity recreation; existing activity-scoped ViewModels retain Home month and transaction month/query/type during these transitions. This task does not add process-death persistence for the existing transaction form/filters. Add is an action, not a selected primary destination. Back/cancel from secondary routes returns to the invoker.

View all explicitly selects the Home month and clears transaction query/type filters; ordinary tab changes do not. Successful writes and missing-record returns discard secondary history by popping to Home and restoring/creating Transactions, so no prior list visit is required. Detail/edit retain existing back controls. Statistics and Settings intentionally expose only a localized future-feature message; their business features remain deferred.

`presentation -> domain <- data`; `di` connects implementations to domain interfaces at the app boundary. Domain must not import Android, Compose, Room, Hilt, or presentation/data classes. Data must not depend on presentation. Compose screens must not call DAOs or know Room entities. These package rules are enforced by review while the code remains in one module.

AGP 9 supplies Kotlin compilation for the Android module; only the Compose compiler, KSP, and Hilt plugins are applied separately. Their versions and all new library versions live in the Gradle version catalog. Room's runtime, KTX, and KSP compiler generate the TASK-002 database and DAO; the version 1 schema is exported under `app/schemas`.

## Data and state flow

TASK-004's Home completion adds `MonthlyBudgetRepository` and observe/save/calculate budget use cases in domain. `PreferencesMonthlyBudgetRepository` persists one positive whole-VND limit per `YearMonth` in app-private SharedPreferences, exposes changes through a cancellable Flow, and performs checked writes on IO. Hilt wires storage separately from the pure use cases. This small preferences store does not change the Room transaction schema or introduce a dependency.

HomeViewModel owns one transaction observer and one budget-limit observer. Budget use derives from the same selected-month expense total as the summary; summary, recent rows, and budget are published together on transaction updates. Percentage uses exact BigInteger arithmetic; only the visual progress bar is capped at 100%. Unset, loading, read/write error, overflow, and explicitly stale transaction data remain distinct. Saving is validated and blocks duplicate writes. Budget/category/notification sheets receive state and callbacks; they never access repositories. Categories reuse the transaction form's localized suggestions plus saved categories; selecting one prefills the existing Add flow. The bell shows actual selected-month budget notices without OS delivery or fabricated history.

Room is the local source of truth. `TransactionDao` exposes a Room `Flow`; `RoomTransactionRepository` maps entities to domain records and implements the domain repository contract. Pure domain use cases validate writes and expose the observation stream. `TransactionViewModel` collects that stream in `viewModelScope`, exposes immutable `StateFlow<TransactionUiState>`, and handles user actions; Compose collects state with lifecycle awareness, renders it, and sends events. Navigation Compose owns list, add, detail, and edit routes. Use suspend functions for writes and explicit error handling for failures. Keep amounts as positive `Long` values in whole VND and dates as `LocalDate` in domain; the mapper stores dates as epoch-day integers and presentation formats values for display.

TASK-006 keeps the existing single Room transaction observer. `TransactionViewModel` filters each snapshot and filter change through pure `FilterTransactionsUseCase` and publishes visible records with the full snapshot in `TransactionUiState`. Note/category substring search is case-insensitive using `Locale.ROOT`, trims query whitespace and keeps accents significant. Type, exact category and inclusive `TransactionDateRange` combine with AND; a custom range replaces the selected month. Filtering preserves Room's date/id ordering and never writes data. Category choices derive from all saved records plus any retained selection, independently of current matches. The Material 3 filter sheet owns saveable drafts; Apply commits category/range together, while dismissal discards drafts. UTC epoch-day conversion keeps picker selections independent of the device timezone. No schema, repository API or dependency change is needed for this offline data set.

## Localization

Vietnamese is the default resource locale (`values/strings.xml` and `res/resources.properties`); English translations live in `values-en/strings.xml`. User-facing text belongs in Android string/plural resources with matching keys and placeholders. Date, number, and currency display formatting belongs in presentation, while domain values remain locale-neutral.

AGP generates the locale configuration for Android 13+ system per-app language settings. The app filters packaged language resources to `vi` and `en`, so library translations cannot add unsupported languages to the generated list. The app does not provide its own language picker; that belongs to a future Settings task.

## Statistics (TASK-007)

`CalculateMonthlyStatisticsUseCase` is pure Kotlin and reuses `CalculateMonthlySummaryUseCase` for six chronological months ending in the selected month. Exact checked Long totals and saved-category expense aggregation produce `MonthlyStatistics`; shares are locale-neutral Double fractions with no integer multiplication/division. Zero expense produces no slices; overflow is surfaced rather than wrapped. No Room schema or repository contract changes are needed.

`presentation.statistics.StatisticsViewModel` observes the existing `ObserveTransactionsUseCase` once, caches each successful snapshot, and publishes selected-month totals, categories and comparison together in immutable StateFlow. Month changes reuse the snapshot and persist `statistics_selected_month` via SavedStateHandle, independently of Home/list filters. Retry cancels/replaces the observer; load failures discard the cache and hide data until a fresh emission. Calculation overflow hides the charts but keeps observing so a later valid snapshot or month can recover. Cancellation is rethrown.

`StatisticsScreen` accepts state/callbacks without repositories or navigation. Compose Canvas draws dynamic donut segments and layout primitives draw paired bars using the same scale. Accessible text lists each category/percentage/exact amount and each month's exact income/expense. Existing top bar/month selector, error banner, icons and theme are reused. Locale formatting and compact donut-center display stay in presentation; exact whole-VND amounts remain visible in totals and legends. No dependency is added.

## Testing

Test domain rules with local unit tests; test repository mapping and observable updates when Room is introduced; add focused UI tests for important user flows. Each roadmap task names the checks that prove its acceptance criteria.
