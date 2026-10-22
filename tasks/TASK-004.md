# TASK-004: Home UI + MVVM Integration

**Status:** Home is implemented. Bottom navigation was removed from this task at the user's request on 2026-10-24 and deferred to a separate branch. Verification of this scope change is recorded at the end; earlier verification records describe their historical code. API 26 startup and a spoken TalkBack audit remain unverified.

### Home naming alignment (2026-10-24)

The user requested that presentation naming match Home in Figma/navigation. Rename the presentation package, screen/ViewModel/state and helper types, test classes/files, resource keys, test tags, local identifiers, and documentation references to Home. Keep the monthly-summary/budget domain names and behavior unchanged. Preserve the existing saved-month storage key for state compatibility and retain historical screenshot filenames as verification evidence. Acceptance: all current Home references compile, both locales retain matching resource keys/placeholders, existing state/navigation tests pass, and build/unit/lint results are recorded below.

### Earlier delivery: HomeScreen UI only (2026-10-23)

The earlier delivery covered only the Compose screen. The current request authorizes the full TASK-004 integration below, reusing that screen and the existing domain, repository, DI, and ViewModel work.

Observable UI acceptance criteria:

- Render the selected month and forward previous/next events; render the supplied monthly summary and recent records without calculations, sample financial data, or direct repository access.
- Keep loading, observed-empty, load failure, stale data, retry, and calculation overflow distinct. Hide financial values when the supplied summary belongs to another month or cannot be calculated.
- Forward Add, View all, record ID, and Retry through required callbacks. Navigation and lifecycle-aware StateFlow collection remain the caller's responsibility in the later integration slice.
- Reuse the shared theme, AppTopBar, category icons, transaction row, error banner, and date/month formatting. Extract MonthSelector for use by both Transactions and Home; move the existing row unchanged into TransactionComponents so both screens share it.
- Provide matching VI/EN strings, locale-aware whole-VND totals, scrollable content, scalable text, and accessible month controls/error announcements.

Figma Home `3:9610` was rechecked through MCP design context, metadata, and screenshot. The earlier UI slice stacked income/expense totals and exposed Add through a floating action button. The current app uses floating/empty-state Add actions without bottom navigation; the later visual correction restores two-column totals with a measured fallback for long amounts/larger text. No Figma modification is required.

**Outcome:** Let users open a Figma-aligned monthly Home, see real income/expense/net/recent transactions, configure a persisted monthly budget and track actual spending, select categories to start Add, and view budget notices without losing navigation context.

## References and current state

- Follow [AGENTS.md](../AGENTS.md), [PROJECT.md](../docs/PROJECT.md), [ARCHITECTURE.md](../docs/ARCHITECTURE.md), [DESIGN.md](../docs/DESIGN.md), and [ROADMAP.md](ROADMAP.md). This specification expands the roadmap's TASK-004 monthly summary into a Home with recent transactions and navigation; the selected-month calculation and month-boundary requirements remain part of the task.
- TASK-002 provides `TransactionRepository`, `RoomTransactionRepository`, a Room observation stream, and transaction use cases. Amounts are positive whole-VND `Long` values; transaction dates are `LocalDate`. The DAO orders records by date descending, then ID descending. Reuse this persistence path without a schema change.
- TASK-003 provided list, add/edit, detail, validation, confirmation, and transaction ViewModel tests. Its transaction-only navigation started on Transactions and assumed that the list already existed on the back stack. TASK-004 replaces that host with `presentation.ExpenseManagerApp`, starts on Home, and explicitly navigates to Transactions after successful writes.
- Before this integration, the working tree already supplied HomeScreen, HomeUiState, HomeViewModel, exact monthly-summary domain arithmetic, DI, and initial tests. MainActivity displayed Home with empty navigation callbacks. This task connects the app, fixes month/error/retry state transitions, and extends verification rather than duplicating those implementations.
- Reuse `ui/theme/Color.kt`, `Type.kt`, and `Theme.kt`, plus `presentation.common.components.AppTopBar` and `MonthSelector`. Shared transaction row rendering, category icons, and error UI are in `TransactionComponents.kt`; date/VND/month formatting is in `TransactionFormatting.kt`. Preserve their existing behavior.

## Scope

### User-requested Home completion (2026-10-23)

The latest user request explicitly adds the missing Home greeting/avatar, notification action, Budget/Categories shortcuts, and monthly budget card. This supersedes the earlier exclusions for those Home elements. Deliver a real per-month budget limit that users can set/edit/clear and retain across relaunch, using a small preferences repository behind pure domain contracts/use cases. Reuse observed transaction expenses to calculate budget use, remaining/over-budget amounts, and percentage. An unset budget is explicitly unset, not a fabricated limit or percentage. Keep the transaction Room schema unchanged.

The Categories shortcut opens the existing localized suggestions plus categories from stored records and can start the existing Add form with that category/type. The bell opens current-month budget notices derived from actual expenses (or a genuine empty notice state); no mocked unread count, notification history, or OS delivery is introduced. Greeting/date use the device clock and active locale; the avatar is neutral because no user profile/name exists. Retain selected-month controls, existing navigation, VI/EN, lifecycle StateFlow, exact arithmetic, accessible controls, and scrolling.

To restore the Figma greeting position without adding another permanent header row, the date/month line opens a month picker containing the shared previous/next selector. For the current month it shows today's date; for another selected month it shows the month/year. It remains available during loading/error and announces the selected month accessibly. Budget/category/notification panels are small working Material 3 sheets; this request does not introduce the full secondary-screen product scope.

Acceptance: every requested Home block is present in Figma order; Budget and Categories actions and bell work; saving/clearing a budget updates the card and survives relaunch; create/edit/delete expenses update budget use; month changes select the matching limit; unset/loading/error/stale states never fabricate financial values; VI/EN and large text remain usable; relevant build/unit/lint/device checks are recorded. Full budget/category management screens, user profiles, push/local OS notifications, Statistics, Wallets, and Settings remain deferred.

- Keep one `:app` module, Kotlin, Jetpack Compose, Material 3, Hilt, Navigation Compose, and minimum SDK 26. Use existing dependencies; add a dependency only for a concrete requirement through the version catalog.
- Implement `HomeScreen`, immutable `HomeUiState`, and a Hilt-injected `HomeViewModel` under `presentation.home`. Keep calculation rules in pure domain code and persistence behind the existing repository contract.
- Display a selected month, previous/next month controls, total income, total expense, net income, and recent transactions for that month. Start with the device's current calendar month on a fresh launch.
- Connect Home and the existing transaction routes through app-level navigation. Home becomes the launch destination; View all opens Transactions; floating/empty-state Add opens the form. Bottom navigation is deferred to a separate branch.
- Provide Vietnamese default and English resources, locale-aware formatting, accessible controls, and distinct loading, observed-empty, content, and error states.
- During implementation, update architecture/design documentation and the TASK-004 roadmap entry to describe the delivered Home and navigation decisions. This planning change creates only this task document.

## Figma and Design System

The [Home frame `3:9610`](https://www.figma.com/design/Ej8vNW8NfvMc3XFw3WfQ6V/personal-expense-manager?node-id=3-9610) was consulted through Figma MCP metadata and screenshot on 2026-10-22. It shows a title, greeting/avatar, purple balance card with monthly income and expense, budget/category shortcuts, budget progress, two recent rows with a View all action, and five bottom destinations. The frame is a populated Vietnamese light screen, not a specification for empty/error states or navigation outcomes.

Use these explicit adaptations during implementation:

- Reuse the existing muted pastel palette, typography, light theme, and solid fills. Use the observed 20 px content margins, 16 px section gaps, 20 px card radius, and 44 px category icons as references; allow content to grow for device width and font scale. Use Material 3 Scaffold, cards, navigation, buttons, and progress indicators where appropriate.
- Use `AppTopBar` with a localized Home title and working budget-notice bell. Keep the current app identity; resolving the Figma name PocketBalance is outside this task. Use a clock-based generic greeting and neutral avatar because no user profile exists.
- Adapt the balance card into a monthly summary card: label the main value **Chênh lệch thu chi / Net income**, with income and expense below it. This value is selected-month income minus expense, not a wallet balance, opening balance, or all-time balance. Omit the wallet shortcut and chevron.
- Open the shared month selector from the date/month line above the summary. Preserve selectable-month behavior while keeping the Figma greeting layout.
- Retain the recent-transactions section and View all action. Use up to five rows instead of the two sample rows; reuse transaction row/category styling and actual stored fields. The model has no transaction time or account, so do not fabricate the mockup's time or wallet data.
- Omit bottom navigation from this task. Keep the existing floating/empty-state Add actions, View all, and Android Back. Include working Budget/Categories shortcuts and real monthly budget progress as authorized in the Home completion scope.
- Recheck the Home frame through Figma MCP before implementing UI. Record later design deviations and VI/EN device review evidence here. No changes to the Figma file are required.

## Calculation and data flow

### Monthly rules

1. Select records by their stored calendar date: `YearMonth.from(record.date) == selectedMonth`. Include the first and last day of the month; exclude adjacent months and other years. Do not convert these date-only values through UTC timestamps.
2. `incomeVnd` is the sum of `amountVnd` for income records in that month. `expenseVnd` is the corresponding positive sum for expense records. `netVnd = incomeVnd - expenseVnd`; negative and zero net values are valid.
3. A successfully observed month with no records has income, expense, and net equal to zero. Search text and type filters on Transactions must not affect Home calculations.
4. Use exact integer arithmetic; no `Double`/`Float` conversion or formatted strings in domain. Detect a total outside `Long` range and surface a localized calculation error rather than wrapping or silently clamping. Do not change transaction validation limits or the Room schema as part of this task.
5. Recent transactions are the first five records from the selected month, sorted by date descending, then ID descending. Show all records when fewer than five exist. The limit applies only to the recent list; summary totals include every record in the month. Future-dated records in that month participate under the same rules.

For example, in October 2026, income of 20,000,000 VND and expenses of 8,200,000 VND produce net income of 11,800,000 VND. A September 30 or November 1 record does not affect October. Income of 100,000 VND and expense of 150,000 VND produce net income of -50,000 VND.

### Responsibilities

- Add a pure monthly summary model and calculation use case in `domain`. Domain may use Kotlin, `java.time`, and the existing coroutine/Flow contracts, but imports no Android, Compose, Room, Hilt, or presentation/data types.
- Reuse `ObserveTransactionsUseCase` -> `TransactionRepository` -> `RoomTransactionRepository` -> `TransactionDao` as the observation path. Provide new domain use cases through `di`; the Home ViewModel must not call a DAO or concrete Room repository.
- Have `HomeViewModel` observe in `viewModelScope` and expose immutable `StateFlow<HomeUiState>`. State includes the selected month, load/error status, summary, and recent records. Keep mutable state private; UI sends month-change and retry events and collects with `collectAsStateWithLifecycle`.
- Derive summary and recent rows from the same observed record snapshot and selected month so the card and list cannot disagree. Room writes, date/type changes, and deletes must update both without a manual refresh or app restart.
- Own Home month selection independently of transaction search/type filters. Preserve it across recomposition, screen navigation, form/detail round trips, and activity recreation. Month changes must not start duplicate active observers; retry replaces the failed observation and respects coroutine cancellation.
- Keep Compose responsible for rendering and localized formatting. Share formatting helpers where needed; row amounts retain their income/expense sign, summary income/expense show nonnegative totals, and net displays its actual sign without applying a transaction-type sign again.

## Navigation behavior

| Trigger | Expected outcome |
| --- | --- |
| Fresh app launch | Open Home with the current month and start Room observation. |
| View all on Home | Open Transactions for the Home month with empty search and the All type filter, so rows match the Home context. This deliberate action replaces the list's previous filters. |
| Home recent row | Open the existing detail route with the exact record ID, regardless of list filters. Back returns to Home with its selected month retained. |
| Floating Add or empty-state Add action | Open the existing form with its current defaults and validation. Back/cancel returns to the invoking destination without a write. Opening Add does not itself change the Home month. |
| Edit from detail | Reuse the existing edit form, saving the same ID. Cancel returns to detail. |
| Successful save or confirmed delete from any entry path | Return to Transactions as in TASK-003, with Home reachable through Android Back. Ensure the list destination exists or navigate to it explicitly; do not rely solely on popping to a list that may never have been opened. Preserve the existing save behavior that selects the saved record's month. |
| Failed write or missing record | Keep the existing error/retry or unavailable-record behavior; the Back to transactions action must work even when detail was opened directly from Home. |
| Back from Transactions / Home | Return to Home from Transactions; Back from the Home root exits through normal Android behavior. Repeated View all/Back navigation must not create a navigation loop. |

Move/rename the transaction-only navigation host to an app-level presentation location if needed to reflect its expanded responsibility; keep Home logic out of `TransactionViewModel`. Home and Transactions have floating/empty-state Add actions and no bottom-bar slot or component; form/detail/edit retain their secondary-screen back controls. Update existing flow tests to enter Transactions explicitly where necessary after changing the start destination.

## States, localization, and accessibility

| State | Observable behavior |
| --- | --- |
| Loading before first observation | Keep the title, month selector, Add, and View all usable; show progress. Do not present initial zero values as observed financial data or flash the empty state. |
| Ready with records | Show summary and the selected month's recent rows, plus View all and Add navigation. A zero net with records remains content, not an empty state. |
| Ready without records in the month | Show three zero totals, explain that this month has no transactions, and offer Add. Other months may still contain data; keep month navigation and View all usable. |
| Observation failure before any data | Show a localized load error and Retry; do not substitute zero totals or an empty-success message. |
| Observation failure after data | Keep the last successful snapshot with a visible stale-data explanation and Retry. Month changes may derive a new month's snapshot from cached records only if it remains clearly stale; never label the previous month's totals as the newly selected month. |
| Retry / calculation failure | Keep the selected month. Retry shows progress and clears the error only after successful observation/calculation. Overflow shows a localized error without false totals; selecting a representable month or correcting records through Transactions allows recovery. |

- Put titles, summary labels, section labels, navigation labels, month-control descriptions, errors, stale-data messages, and empty-state copy in matching `values/strings.xml` and `values-en/strings.xml` resources with matching placeholders. Use existing app language support; do not add a language picker.
- Format whole VND, dates, and month/year using the active locale in presentation. Never translate or mutate user-entered categories/notes on a locale change. Do not concatenate localized UI sentences from fragments.
- Give controls meaningful accessibility labels, touch targets of at least 48 dp, meaningful action semantics and error announcements. Identify income/expense/net with text and signs as well as color. Support scrolling, system insets, long text/amounts, and enlarged fonts without hiding actions or financial values.

## Acceptance criteria

1. Fresh launch opens Home without a bottom bar. Floating/empty-state Add, View all, recent-row detail, and Android Back follow the table above, including a save/delete when Transactions has never been opened. Repeated View all/Back navigation does not stack duplicate destinations.
2. The summary uses only the selected month's Room records and exactly implements the income, expense, and net rules. Both the positive and negative examples above pass; empty, income-only, expense-only, zero-net, and `Long` overflow cases behave explicitly.
3. Previous/next controls cross month and year boundaries correctly, including leap-year February. Home month survives activity recreation and navigation round trips; transaction search/type filters do not change Home totals or recent rows.
4. Recent rows include at most five selected-month records in date/ID descending order. With six or more records, totals still include every record. Each row shows its actual category, type/date, optional note, and locale-formatted signed amount and opens the correct ID.
5. With a Room-backed observation active, adding, editing amount/type/date, and deleting records updates Home totals and recent rows without restarting the app. Moving a record to another month removes it from the old month and includes it in the new one; deleting the last record produces observed-empty zero totals.
6. Loading, ready-empty, content, initial failure, stale-data failure, and retry are visibly distinct. No failure is shown as an empty success. Retry preserves the selected month, avoids duplicate collectors, and recovers after the source succeeds. Calculation overflow never displays a wrapped value.
7. `HomeViewModel` exposes immutable StateFlow, calls domain use cases, and has focused state-transition tests. Compose uses lifecycle-aware collection and event callbacks; domain has no framework imports and presentation has no direct Room access. There remains one app module and no unnecessary schema/dependency change.
8. Home reuses `AppTopBar`, the shared theme, category icons, rows, and formatting. Figma Home review and intentional adaptations are recorded; surfaces have no gradients. Greeting/avatar, bell, Budget/Categories shortcuts, and monthly-budget card appear in reference order. Budget edits persist; expenses update progress; categories prefill Add; notices reflect actual budget use. Statistics, Wallet, Settings, full budget/category management, and OS notifications remain deferred.
9. Vietnamese and English resources have matching keys/placeholders; labels, errors, dates/months, positive/negative/zero amounts, long content, and larger fonts render readably on a device/emulator. Accessibility labels, 48 dp controls, scrolling, and insets are verified.
10. Existing transaction validation, create/edit/delete, confirmation, missing-record handling, persistence, and filter behavior remain covered after the start-destination change. Required Gradle checks and device evidence are recorded before marking this task complete; historical TASK-003 results are not evidence for TASK-004.

## Testing and verification plan

The Home completion additionally requires exact budget calculations (including limits/expenses at Long boundaries), unset/loading/error/write validation, independent month limits, expense amount/type/date/delete updates, stale-data/overflow recovery, and a single cancellable budget observer. Device tests must verify setup/clear/notices/category-to-Add behavior, actual preferences persistence, VI/EN, and the reference Home geometry. A normal-app relaunch smoke verifies the production preferences binding rather than the integration test's fake budget store.

| Level | Required coverage |
| --- | --- |
| Domain unit | Mixed income/expense, empty month, income-only, expense-only, zero/negative net, values beyond `Int` range, exact `Long` boundary and overflow; first/last day inclusion, adjacent-month exclusion, same month in another year, December/January, leap day. Use fixed dates, not the execution date. |
| ViewModel unit | Delayed first observation -> ready-empty/content; month changes without writes; summary/recent consistency; deterministic date/ID ordering and five-row limit; totals over all rows; create/update/delete emissions; filter independence; initial error, stale error, retry success/failure, selected-month retention, cancellation, and no duplicate observers. Use coroutine test dispatchers and fake repository flows as in TASK-003. |
| Compose state | Render each state; assert zero totals only after successful empty observation; month controls and Add/View all callbacks; recent row ID; Retry callback, stale notice, and financial sign/label semantics. Use stable tags/resources where locale-specific text would make tests brittle. |
| Room + navigation integration | Use the existing isolated in-memory Room/Hilt test setup. Verify launch on Home, empty -> create -> updated summary/recent, edit amount/type/date, confirmed deletion -> updated/empty summary, cancel without writes, View all month/filter handoff, recent -> detail -> back, and direct-from-Home save/delete/missing-record return paths with no pre-existing list entry. Re-run existing transaction flow/state tests. |
| Device/manual review | VI and EN populated/empty/error/retry states; negative net and large amounts; selected-month retention across activity recreation and repeated View all/Back navigation; offline create/edit/delete and relaunch persistence; font scale at least 1.3x, long category/note text, touch targets, accessibility labels, scrolling and system insets. Compare Home to Figma, noting the documented scope adaptations. |
| Build and source review | Debug build, unit tests, lint, connected tests; dependency direction, StateFlow ownership, resource parity, shared Design System reuse, one module, and absence of excluded features. |

Run these commands during implementation and record actual results:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
git diff --check
```

Use `--offline` only when the required dependencies are cached, and record the exact invocation used. Run Android checks on a named device/emulator and record its API level. Include an API 26 startup smoke check when that image/device is available; otherwise carry forward the existing TASK-001 limitation explicitly rather than claiming it passed. Record test counts/failures, lint errors/warnings, navigation and locale review evidence, and unresolved limitations. If a required check cannot run, leave the relevant criterion unverified.

## Implementation order

1. Define and unit-test monthly calculation rules and summary values. Reuse the repository observation contract; add Hilt providers for new domain use cases.
2. Implement and test Home ViewModel/StateFlow, month selection, recent-row derivation, and load/empty/error/retry behavior against fake flows.
3. Consult Figma MCP again; implement Compose with the existing Design System and only the shared-helper extraction needed by both features. Add matching VI/EN resources and focused UI-state tests.
4. Integrate app-level Home/Transactions/Add navigation and existing detail/form flows, address the list-back-stack assumption, and add Room-backed navigation/update coverage.
5. Run required checks, perform device/Figma/localization/accessibility review, and update task verification and architecture/design/roadmap documentation to match the delivered behavior.

## Out of scope

Statistics screens/charts; full budget/category management screens and category-specific limits; Wallet/account balances, transfers, or account fields; category CRUD; Settings and an in-app language picker; user profiles and OS notification delivery/history; sync/authentication; export/backup; currency conversion; a new product name; dark-theme or tablet redesign; extra modules and unrelated refactors. The explicitly requested Home elements and minimal working sheets are included, as specified above. Do not add placeholder destinations or nonfunctional actions for deferred features.

## Verification record

- 2026-10-22: Read repository guidance, roadmap, architecture, design, relevant transaction/domain/data/DI code, shared theme/components, and existing test setup. Consulted Home `3:9610` through Figma MCP metadata and screenshot. Created this specification only.
- 2026-10-23, UI slice: `HomeScreen` implemented against the existing state contract. No ViewModel, domain, data, DI, navigation, dependency, commit, or push changes were made by this slice. Figma Home was rechecked with MCP design context, metadata, and screenshot.
- `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` passed: 18 unit tests, no failures; lint reported 0 errors and 15 warnings in existing manifest/dependency/resource areas. `:app:assembleDebugAndroidTest` also passed. One earlier combined lint/test-APK build hit a lint FileNotFoundException while Hilt sources were being generated; completing the test APK and rerunning lint separately succeeded, as did the final combined build/unit/lint invocation.
- `./gradlew.bat :app:connectedDebugAndroidTest` passed on Pixel_5, Android 15 / API 35: 14 tests, no failures, including 7 Home state tests and the existing transaction/Room/Hilt checks. The Home tests cover loading, observed-empty zeros, month/Add/View all/record/Retry callbacks, negative net, stale data, overflow, mismatched summary month, and VI/EN with large amounts at font scale 1.3x.
- The locale/layout test was also invoked directly through ADB to retain screenshots for visual review. VI/EN images at `app/build/dashboard-vi.png` and `app/build/dashboard-en.png` were inspected: large totals, section labels, long category truncation, and Add actions remain readable. The build directory is local verification output, not a tracked design asset. VI/EN resource keys match; `git diff --check` passed.
- At the end of the earlier UI slice, navigation/StateFlow integration, end-to-end Room updates, recreation, TalkBack, and API 26 startup were unverified. The integration record below supersedes that slice's incomplete status.

### Full integration delivery (2026-10-23)

Changes relative to the working tree at the start of this implementation:

- Reused the existing monthly-summary domain model/use case, Room repository, Hilt providers, Home screen/state/ViewModel, shared theme/components, and transaction flows. No schema, dependency, module, amount-validation, or excluded-feature changes.
- Moved the transaction-only host to `presentation/ExpenseManagerApp.kt`; MainActivity now connects both ViewModels. Home launches first, both StateFlows are collected with lifecycle awareness, and Home/central Add/Transactions navigation works. Successful save/delete and unavailable-record return paths explicitly create the list route when it was never opened. Home remains the single back-stack root.
- Added `showAllForMonth` for the deliberate View all handoff; ordinary tabs preserve transaction filters. Home month remains independent, retained through navigation, saved-state restoration, and Activity recreation.
- Fixed Home month changes after initial failure and during retry: retain the error/pending retry and derive cached data for the actual selected month without restarting observation. Removed the unused `isState` field.
- Added the shared primary navigation bar and two local vector icons; reused the existing Add asset as an Image (tinting this opaque PNG had produced a solid square during visual review). Added matching VI/EN navigation copy and a localized Home title. Month controls now have explicit 48 dp bounds. Capped row amount width so long amounts wrap without eliminating category text.
- Added 8 domain boundary tests, 9 observation/ViewModel tests, 7 Room-backed Home navigation tests, and 1 integrated locale/accessibility state test. Updated the two original transaction flow tests to enter Transactions after Home launch. Existing tests are retained.

Actual verification on the delivered code:

| Check | Result |
| --- | --- |
| `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` | Passed. 35 unit tests, 0 failures/errors. Lint: 0 errors, 15 existing warnings (manifest label, dependency versions, template colors/launcher resources). |
| `.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest` | Passed during integration. The final connected invocation rebuilt the test APK after later changes. |
| `.\gradlew.bat :app:connectedDebugAndroidTest` | Passed on Pixel_5 / Android 15 / API 35: 22 tests, 0 failures/errors/skips, including 7 Home Room/navigation tests and 8 Home state tests. Final run used device Wi-Fi disabled and mobile data already disabled; Wi-Fi was restored afterward. Host Gradle did not use `--offline`. |
| Domain/state coverage | Fixed-date month/year boundaries, leap day, first/last day, negative/zero net, exact Long limits, overflow, five-row ordering with all-row totals, delayed first emission, writes/date/type changes, independent filters, initial/stale errors, retry failure/success, saved state, cancellation, and one active observer. |
| Navigation/Room coverage | Direct-from-Home add/edit/delete/missing-record return, cancelled add/edit/delete, exact recent IDs and back behavior, repeated tab changes, View all month/filter handoff, selected-month retention on recreation, and original transaction/persistence tests all passed. |
| VI/EN and accessibility | 65 matching resource keys and placeholders. Device tests cover both locales at 1.3x text, empty/error/retry, negative net at Long boundary, long category/note, selected navigation semantics, and 48 dp controls. The added bounds test first caught a 40 dp month icon; explicit 48 dp sizing fixed it before the passing final run. |
| Visual review | Rechecked Figma Home `3:9610` using MCP metadata and design context with screenshot. Inspected local `app/build/dashboard-integrated-vi.png` and `dashboard-integrated-en.png` after invoking the integrated layout test through ADB. Solid pastel surfaces, full wrapped financial values, ellipsized user text, visible actions, scrolling, and insets follow the documented adaptations. Build images are untracked verification artifacts. |
| Offline production-app smoke | On the normal debug app with Wi-Fi/data disabled, created expense `TASK004_SMOKE` for 321,000 VND; force-stopped and cold-launched the app; Home retained the row and net -321,000. Edited the same record to income 123,000; Home updated to +123,000. Confirmed deletion returned to Transactions, then Home displayed observed-empty zeros. Removed the smoke record and restored the original Wi-Fi/data settings. Local evidence: `app/build/task004-offline.png`. |
| Source/whitespace review | One `:app` module; domain has no Android/Compose/Room/Hilt imports; production presentation has no DAO/concrete repository imports; no fabricated runtime data or excluded destinations. VI/EN parity and `git diff --check` passed. No commit, push, staging, or Git-history changes were performed. |

Remaining verification limitations:

- API 26 startup remains unverified because no API 26 image/device is installed locally (available images are API 35/36/37.2). This carries forward the TASK-001 limitation; API 35 results do not establish API 26 startup.
- Accessibility semantics, labels, selected states, control bounds, font scaling, scrolling, and insets were checked. A complete spoken TalkBack traversal was not performed, so that portion of manual accessibility review remains unverified.

### Visual fidelity follow-up (2026-10-23)

The user requested a closer match after reviewing the integrated UI. Re-read DESIGN, architecture, project guidance, and the roadmap; consulted Figma MCP design context/screenshots for Home `3:9610`, Transactions `3:9699`, and Add `3:9803`. This changes presentation and its verification only; domain/data/DI/navigation behavior and excluded features remain unchanged.

Observable acceptance criteria for this correction:

- At the reference 393 dp width and 1.0 text scale, the top-bar content is 64 dp, the navigation content row is 80 dp, the monthly card is approximately 174 dp, and recent rows are 64 dp. Content margins, card radius/padding, line heights, arrow/indicator sizes, and section spacing follow DESIGN.md.
- Monthly income/expense render as two equal columns with the Figma arrows and localized month labels. They use the full available width when actual measured text cannot fit two columns. Exact negative/Long-boundary amounts remain visible at 1.3x text in VI/EN.
- Month calendar and label are centered together. Navigation uses 11 sp labels, a 4 dp icon/label gap, correctly sized indicators, selected tab semantics, an Add button action, and separate system insets. Touch bounds stay at least 48 dp.
- Recent row notes appear as main titles, with actual category/date below, signed amounts, and localized type accessibility semantics. Icons remain sharp at device density by using original Figma vector paths. No mockup time/account/profile/budget data is introduced.

Delivered changes are detailed in `docs/DESIGN.md` under the visual fidelity correction. Shared typography now keeps the entire 1.35x line-height frame instead of Compose's default trimming; the summary card matches the reference height. Thirteen exported SVGs are local vectors, including five category icons and the two chevrons replacing low-resolution PNGs. Two monthly-label resources were added in VI/EN, and the obsolete note-concatenation resource was removed; 66 keys/placeholders match.

Verification on the corrected UI:

- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` passed: 35 unit tests, no failures/errors; lint 0 errors, 15 existing warnings. `git diff --check` passed.
- `.\gradlew.bat :app:connectedDebugAndroidTest` passed on Pixel_5 / Android 15 / API 35: 23 tests, no failures/errors/skips. The new reference-layout test asserts 64/80/174/64 dp geometry, side-by-side normal totals, note title rendering, and the exact-value full-width fallback at 1.3x. Existing state and Room/navigation flows were rerun after the shared typography/icon changes.
- The isolated screen fixtures, rather than runtime sample records, produce `app/build/dashboard-figma-vi.png` (393 dp reference layout) and `dashboard-integrated-vi.png` / `dashboard-integrated-en.png` (1.3x, negative Long-boundary values). These untracked images are used for Figma comparison. The original scope adaptations and API 26/spoken TalkBack limitations above still apply.
- Rebuilt the Android test APK and invoked the three screenshot/layout tests directly through ADB after making their window setup match MainActivity and aligning configuration/density font scale. All 3 passed. Inspected the final VI reference and VI/EN large-text images; vectors are sharp, standard totals use two columns, large totals wrap completely, and navigation stays visible. Cold-launched the normal debug app and retained `app/build/task004-visual-runtime.png` for an observed-empty runtime check. Pixel_5 reports a 136 px status/cutout inset at 440 dpi (about 49.5 dp), whereas Figma reserves 28 px: this device-driven offset is preserved rather than drawing beneath the camera/status area.
- No dependency/schema/module changes, staging, commit, push, or Git-history changes were made.

### User-requested Home completion (2026-10-23)

This delivery supersedes the earlier Home-element exclusions. Re-read repository/design guidance and consulted Figma MCP Home `3:9610` again. Changes relative to the preceding visual correction:

- Added the original Figma bell and green/yellow Budget/Categories shortcuts as local vectors; added a neutral avatar, localized clock-based greeting/date, and the white monthly-budget card. The date/month line opens the shared month selector in a dialog. Existing Design System colors, typography, spacing, summary, recent rows, navigation, and transaction flows are reused.
- Added pure monthly-budget model/repository contract and observe/save/calculate use cases. A small IO-backed preferences implementation retains independent positive whole-VND limits per month without changing Room or dependencies. No runtime sample name, limit, percentage, or financial record is introduced.
- Extended Home StateFlow with validated setup/edit/clear, separate loading/error states, cancellable budget observation, exact percentage/remaining values, and budget data derived from the summary's transaction snapshot. The card updates on amount/type/date/deletion changes. Overspending remains visible and overflow hides unavailable financial values; failed source reads retain only clearly marked stale data.
- Added working sheets: Budget edits/removes the selected month's limit; Categories reuses form suggestions and saved transaction categories to prefill the existing Add flow; the bell shows reached/exceeded budget notices or a real empty state. Full secondary management screens and OS notifications remain deferred.
- Added 12 focused unit tests (6 domain and 6 ViewModel), 2 real preferences device tests, and 2 Home integration flows. Extended the existing Figma reference assertions to include 92 dp budget geometry, exact 68% fixture text, greeting, bell, and shortcuts. Fixed an existing missing-record test's asynchronous Room wait and used the unmerged semantics tree to inspect text inside the clickable budget card.
- Updated PROJECT, ARCHITECTURE, DESIGN, ROADMAP, and the current task scope/criteria to describe the delivered Home behavior. Matching VI/EN keys and placeholders now total 90.

Verification on the completed Home is recorded below; the earlier delivery counts above are historical.

| Check | Final result |
| --- | --- |
| `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` | Passed on the completed code, including a rerun after the device-test corrections. 47 unit tests, 0 failures/errors. Lint: 0 errors, 15 existing warnings and 1 hint. |
| `.\gradlew.bat :app:connectedDebugAndroidTest` | Passed on Pixel_5 / Android 15 / API 35: 27 tests, 0 failures/errors/skips. Includes real preferences persistence/corruption checks, 9 Home Room/navigation flows, and 9 Home state/layout tests. |
| Retry history | The first device run identified a missing asynchronous Room wait and a merged-semantics text lookup in tests; both were corrected. A subsequent run was interrupted by an emulator Binder-service hang, with `/data` still having 7.8 GB free. Restarted the emulator without wiping data; the final complete Gradle invocation passed in 1m 28s. Interrupted/failing runs are not counted as passes. |
| Figma and locale/layout review | Invoked the 3 screenshot/layout tests directly through ADB after the final connected run: all passed. Inspected `app/build/dashboard-figma-vi.png` against the MCP Home screenshot; the new blocks are present, with 48 dp shortcuts/bell and a 92 dp populated budget card. Also inspected the VI/EN 1.3x large-amount scroll fixtures at `app/build/dashboard-integrated-vi.png` and `dashboard-integrated-en.png`. These use test fixtures, not production sample records. |
| Production persistence smoke | Cold-launched the normal debug app with actual per-app locale VI. The month dialog showed localized title, previous/next labels, and confirmation. Set October 2026's limit to 12,000,000 VND through the UI, force-stopped and cold-launched, and verified the stored preferences entry, 0% real spending, 12,000,000 VND remaining, and retained editor input. Removed only that test limit through the UI; the card returned to unset and the preferences file became empty. Restored the original app locale list `[]`. No transaction records were created. Runtime screenshot before cleanup: `app/build/home-complete-runtime.png`. |
| Source review | 90 matching VI/EN keys and placeholders; domain remains free of Android/Compose/Room/Hilt and data/presentation imports; presentation has no direct data/DAO imports. One module, no new dependencies or Room schema changes. `git diff --check` passed. No staging, commit, push, or Git-history changes. |

The existing API 26 startup and complete spoken TalkBack limitations remain open. The new production relaunch smoke did not toggle networking; the earlier recorded transaction offline smoke remains separate evidence. Screenshot/build artifacts are local verification output and are not tracked design assets.


### Bottom navigation deferred (2026-10-24)

The user moved bottom navigation to a future separate branch. This scope supersedes earlier bottom-bar and tab-selection requirements and their visual/test records.

Observable acceptance criteria:

- No bottom-bar component, screen slot, tab callback wiring, dedicated icon, or Add-tab string remains in production code.
- Home still launches first. Floating/empty-state Add opens the form; View all opens Transactions with the Home month and cleared list filters. Android Back returns to Home.
- Direct-from-Home detail, save/delete/missing-record returns, category-prefilled Add, budget behavior, and month restoration continue to work.
- Existing UI/navigation tests exercise those entry points without bottom-bar selectors; layout checks retain the Home card/top-bar/accessibility assertions.

Actual verification of this scope change (Android Studio bundled JBR):

| Check | Result |
| --- | --- |
| `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` | Passed. 47 unit tests, 0 failures/errors/skips. Lint: 0 errors, 15 existing warnings. Android test APK compiled successfully. |
| `.\gradlew.bat :app:connectedDebugAndroidTest` | Passed on Pixel_5 / Android 15 / API 35: 27 tests, 0 failures/errors/skips. Existing flows now use Add, View all, and Android Back; the repeated-navigation test verifies the month/filter handoff without tabs. |
| Source/resources | No bottom-bar component, slot, tab selector, or dedicated icon/string references remain under `app/src`. VI/EN resource names match (89 each). |
| Whitespace | `git diff --check` for the changed files passed. The full working-tree check still reports a pre-existing extra blank line at EOF in `MonthlySummaryCard.kt`; that unrelated user edit was preserved. |

Figma Home was consulted again through MCP design context before this change. Automated layout/accessibility tests passed with the bar removed; no fresh manual screenshot review was completed. Historical screenshots above describe earlier code. API 26 startup and complete spoken TalkBack review remain unverified. No staging, commit, push, or Git-history changes were made.

### Home naming verification (2026-10-24)

- Moved the presentation package to `presentation.home`. Renamed the screen, ViewModel, UI state, load/error/category/action types, local identifiers, resource keys, test tags, test classes/files, and current documentation references to Home. `HomeViewModelTest` now lives under its matching `presentation/home` package directory. Domain/data contracts and the monthly-summary/budget names and behavior are unchanged.
- Retained the existing saved-month key with an explanatory comment; existing restoration tests still exercise that key. Historical screenshot filenames above remain unchanged; newly generated test screenshots use the `home-` prefix.
- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` passed with the system JDK 17. 47 unit tests, 0 failures/errors; lint 0 errors, 15 existing warnings and 1 hint. The renamed Home test classes appear in the unit reports and the Android test APK compiles.
- `.\gradlew.bat :app:connectedDebugAndroidTest` passed on Pixel_5 / Android 15 / API 35: 27 tests, 0 failures/errors/skips, including 9 `HomeFlowTest` and 9 `HomeScreenStateTest` cases. Existing financial, budget, category, month/restoration, navigation, and VI/EN layout assertions were reused; no tests were added for a naming-only change.
- Source review found no old presentation package, class, resource-key, or test-tag references. Both locales have 89 matching resource names, no duplicate names, and matching placeholders. `git diff --check` passed for tracked diffs; the pre-existing blank line at the end of the moved `MonthlySummaryCard.kt` was preserved. No staging, commit, push, or Git-history changes were made. Existing API 26 and spoken TalkBack limitations are unchanged.
