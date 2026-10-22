# TASK-003: Transaction UI & MVVM Integration

**Status:** Implemented; Build, Unit Test, Lint, and Android UI Test passed on 2026-10-20. See verification record below.

**Outcome:** Let users browse, create, edit, and delete income and expense records offline through a usable Compose interface backed by the existing transaction use cases.

## Scope

- Replace the starter greeting with the Transactions entry screen. Add transaction list, add/edit form, and transaction detail screens using Jetpack Compose and Material 3. Keep the app in the existing `:app` module and follow the Clean Architecture package boundaries in `docs/ARCHITECTURE.md`.
- Follow the [design reference](../docs/DESIGN.md) and inspect its linked Figma frames before implementing UI. Use the Transactions, Add transaction, and Transaction details frames as the visual references. Reuse the add form for editing, with existing values prefilled.
- Reuse the existing shared Design System in `ui/theme/Color.kt`, `ui/theme/Type.kt`, and `ui/theme/Theme.kt`. Extend it only for a concrete transaction-screen need; avoid duplicating colors, text styles, or theme setup inside individual screens. Use solid fills only; no gradients.
- Add ViewModels and explicit UI state for observation and writes. ViewModels call domain use cases; Compose sends events and renders state. Presentation must not use Room entities or DAOs.
- Let users select income or expense, enter a positive whole-VND amount, choose or enter a nonblank category, select a date, and optionally enter a note. The current domain model has no wallet or account field, so omit the Figma account control until a later data task defines it.
- Support opening a record from the list, editing it, and deleting it after confirmation. Define navigation outcomes: a successful save returns to the list; a successful delete returns to the list; cancel or back leaves stored data unchanged.
- Add transaction list search, income/expense type filters, and month selection shown in the Figma reference, applied to the observed records. Preserve these selections when returning from the form or detail. Monthly financial totals remain TASK-004.
- Localize every visible transaction-screen string in default Vietnamese and English resource sets with matching keys and placeholders. Format VND and dates for the active locale in presentation; provide accessible labels and layouts that tolerate scrolling and font scaling. Add dependencies only when needed through the version catalog.
- Keep list, add/edit form, and detail composables in separate files named after each screen. Keep screen-specific helpers private in their screen file and transaction-specific shared UI helpers in `TransactionComponents.kt`. Keep the feature-independent `AppTopBar` in `presentation/common/components` for reuse across features.

## Acceptance criteria

1. Launching the app opens Transactions. A fresh database shows a distinct observed-empty state with an Add action; loading remains visible until the first observation. A search or filter with no matches says no records match and keeps the current query, type, and month.
2. The list renders observed records in repository order (date descending, then ID descending). Each row shows type, category, date, optional note when present, and a correctly signed and locale-formatted VND amount. A Room-backed change appears without restarting the screen.
3. Search matches category and note case-insensitively. Type selection shows all, income, or expense. Month selection includes only records whose `LocalDate` falls in the selected calendar month; moving between months does not alter stored records. Clear or change controls restore matching rows.
4. The add form starts with no amount and sensible type/date defaults. Required fields are clear. Amount parsing accepts positive whole VND within `Long` range and rejects blank, zero, negative, fractional, or overflowing input before any write. A blank or whitespace-only category is rejected. Errors are shown next to the relevant field, and invalid input leaves the user on the form.
5. Saving a valid income or expense creates exactly one record with the chosen type, amount, category, date, and optional note. While a save is in progress, duplicate submission is blocked. The new record appears in the list after the Room observation updates.
6. Opening a row shows its stored fields. Edit opens the shared form prefilled from that record. A successful edit updates the same ID without creating a second record; cancel or back makes no change. If the record has been removed, detail and edit show an unavailable state with a route back to Transactions.
7. Delete requires explicit confirmation naming the action. Cancel keeps the record. Confirmed deletion removes only the selected ID and updates the list. An unknown ID is not reported as a successful deletion.
8. Observation and write failures show a readable, accessible error and a retry path. A failed save or delete keeps the form/detail context and entered values, does not report success, and allows another attempt. Loading, empty, and error are distinct states as described in `docs/DESIGN.md`.
9. All visible text comes from matching Vietnamese default and English resources, including validation, empty, loading, confirmation, and error copy. Both languages render without missing or mismatched placeholders. Dates and amounts follow the active locale in presentation; domain and database values stay locale-neutral. Controls have meaningful accessibility labels and usable touch targets; form content remains reachable with the keyboard, system insets, and larger font settings.
10. The code preserves the Clean Architecture direction `presentation -> domain <- data`: ViewModels depend on domain use cases, the domain has no Android/Compose/Room/Hilt imports, and Compose has no direct Room access. Transaction screens use Material 3 components and reuse the existing `ui.theme` colors, typography, and theme rather than defining duplicate screen-local versions. All transaction UI surfaces use solid fills with no gradients. `settings.gradle.kts` still includes only `:app`.
11. Focused tests or documented device checks verify the create, edit, delete, validation, list update, filtering, and empty/loading/error flows. Run `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug`; run relevant Android UI tests with `:app:connectedDebugAndroidTest` when added. Record exact commands, results, and device API level before marking this task complete. Do not claim a check passed unless it ran.
12. `TransactionListScreen.kt`, `TransactionFormScreen.kt`, and `TransactionDetailScreen.kt` each own their screen and private helpers. Shared error and unavailable states live in `TransactionComponents.kt`. Extracting the screens preserves their signatures, navigation, UI state, resources, and existing flow tests.
13. List, add/edit, and detail use the same `presentation.common.components.AppTopBar`. It has no transaction imports and accepts a title, optional back callback, optional action content, and a `Modifier`. Extraction preserves the existing title styling, colors, back label, actions, and navigation behavior.

## Test plan

| Level | Evidence |
| --- | --- |
| Local unit | ViewModel state and events with fake use cases/repository: first observation, empty and filtered-empty results, search/type/month filtering, valid and invalid form input, duplicate-save prevention, failed writes, and unknown IDs. |
| Android UI | Focused Compose or documented manual checks for opening the list, creating both record types, editing one ID, canceling and confirming delete, keyboard/inset behavior, accessibility labels, and Vietnamese/English display. |
| Build and lint | Run `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug` with the Gradle wrapper. Run connected tests if the UI suite is added. |

## Out of scope

Monthly income, expense, and balance calculations belong to TASK-004. Home, Statistics, Budget, and Wallet screens or behavior are outside TASK-003, including their navigation destinations. Charts, transfers, category management, sync, authentication, currency conversion, and an in-app language picker are also outside this task. Do not add placeholder destinations or controls for these features.

## Four implementation phases

These are milestones within TASK-003, which remains one reviewable task. Complete the checks for each phase before moving to the next; record the final verification results in this file.

### Phase 1 — Shared Design System

**Status:** Complete. Reused the existing shared theme. Inspected Figma Transactions `3:9699`, Add `3:9803`, and Detail `3:9871` through Figma MCP. Reviewed normal and 1.3× font display on Pixel_5 API 35; solid fills and readable text confirmed.

- Use `docs/DESIGN.md` and its linked Figma frames as the source for the shared light palette, typography, spacing, and reusable component guidance. Inspect the relevant Figma frames before implementing transaction UI.
- Keep the observed colors in `Color.kt`, the shared Material 3 typography in `Type.kt`, and the light color scheme in `Theme.kt`. Add a shared value or component only when the transaction screens need it; keep the current no-gradient direction.
- Review the existing theme against the design reference, including text contrast, font scaling, and use in Compose previews or a device screen. Record any corrected measurements or decisions in `docs/DESIGN.md`.

#### Acceptance criteria

1. `Color.kt`, `Type.kt`, and `Theme.kt` provide one shared light Material 3 theme for the transaction screens; sampled colors and text styles agree with `docs/DESIGN.md` or a documented correction to it.
2. A Compose preview or device screen demonstrates the shared theme with readable text at normal and enlarged font size. Theme and screen surfaces use solid fills; source review finds no gradient brush in the transaction UI.
3. The relevant Figma frames have been inspected before Phase 2 implementation. Any design decisions needed for edit and delete confirmation are recorded in `docs/DESIGN.md` or this task.
4. Record the visual review and relevant build check result before marking Phase 1 complete. Existing theme files alone do not satisfy this criterion.

### Phase 2 — Transaction UI theo Figma

**Status:** Complete. List, add/edit, detail, category suggestions, date picker, search/type/month controls, confirmation dialog, and distinct loading/empty/no-match/error states implemented with Material 3. Vietnamese and English screens reviewed on device.

- Inspect the Transactions, Add transaction, and Transaction details Figma frames linked from `docs/DESIGN.md` before implementing their UI. Build the transaction list, add form, and shared edit form using the existing Material 3 theme and reusable components.
- Provide the visible list controls from the reference: search, income/expense filters, month selector, transaction rows, and Add entry point. Render loading, empty, no-match, and error presentations specified in `docs/DESIGN.md`.
- Provide the record detail or equivalent route needed to edit and delete a selected transaction. Show a confirmation dialog before deletion; cancel closes it without changing the record. Define the expected screen transitions and back behavior for Phase 3 to wire.
- Add matching Vietnamese and English UI strings, accessible labels, scrolling, and keyboard/inset handling for these screens as they are built. Use previews or UI checks with representative state, including both transaction types and long text.

#### Acceptance criteria

1. The Transactions UI renders representative populated, loading, no-records, no-matches, and error states in previews or a UI harness. List rows, search/type/month controls, and Add entry point are visible and reviewable against the Figma Transactions frame.
2. The add form renders empty input and field-error states; the edit form renders a prefilled transaction. Type, amount, category, date, and optional note fields are reachable with the keyboard and on a scrolling screen.
3. A selected transaction can display the edit and delete entry points. Delete confirmation presents confirm and cancel actions; cancel closes the dialog in the UI harness without invoking deletion.
4. The screens reuse the shared theme and Material 3 components, contain no gradients, and render all visible copy from matching Vietnamese and English resources. Record preview or device evidence for both locales.
5. The UI contains only transaction destinations for this task; Home, Statistics, Budget, and Wallet screens and navigation entries are absent.

### Phase 3 — MVVM Integration

**Status:** Complete. `TransactionViewModel` exposes immutable `StateFlow<TransactionUiState>`; Navigation Compose connects list/add/detail/edit. Hilt-injected TASK-002 use cases observe Room and perform validated writes. Integration test covers create, edit, cancel delete, confirm delete, and list updates.

- Add ViewModels and explicit `UiState` for the transaction list, form, and selected-record flow. Expose immutable `StateFlow` to Compose; collect it with lifecycle-aware behavior and send user actions back as events.
- Wire Navigation among the list, add, detail, and edit routes. Keep search, type filter, and selected month when returning to the list. Apply the defined save, delete, cancel, and back outcomes.
- Connect ViewModels to the observe, add, update, and delete UseCases from TASK-002 through Hilt. Room remains the source of truth behind `TransactionRepository`; presentation does not import a DAO, entity, database, or `RoomTransactionRepository`.
- Parse and validate form input before calling write use cases. Block duplicate saves, preserve entered values on a failed write, distinguish loading/empty/error states, support retry, and handle a selected record that has been deleted.
- Verify during integration that creating, editing, and deleting a record changes the Room-backed list without restarting the app. Carry the repeatable test coverage and final review into Phase 4.

#### Acceptance criteria

1. ViewModels expose immutable `StateFlow<UiState>`; Compose observes it with lifecycle-aware behavior. A fresh Room database moves from loading to the no-records state, and an added record appears through the observation stream without restarting the screen.
2. Navigation opens add and edit with the right record data, returns after successful save/delete, and leaves stored data unchanged on cancel/back. The selected month, type filter, and search query remain when returning to the list.
3. Valid form input calls the appropriate TASK-002 use case once and creates or updates the expected record ID. Blank, zero, negative, fractional, and overflowing amounts and blank categories cause a visible field error and no write; rapid repeated save taps do not create duplicates.
4. Delete occurs only after confirmation and affects the selected ID. Missing IDs, observation failures, and write failures display distinct recoverable states; failed writes keep entered data and do not report success.
5. An integration check on a device or emulator demonstrates offline create, edit, delete, and list refresh through Room. Source review confirms presentation imports no DAO, entity, database, or Room repository implementation.

### Phase 4 — Testing và Review

**Status:** Complete. Unit, Compose state, Room, Hilt, and end-to-end Android tests passed; architecture, localization, Figma scope, scrolling, insets, accessibility labels, and font scaling reviewed. Verification and limitations are recorded below.

- Add focused local tests for ViewModel `UiState` transitions, first observation, empty and filtered-empty results, search/type/month filtering, valid and invalid form input, duplicate-save prevention, failed writes, missing records, and unknown IDs.
- Add focused Compose/Android tests where they meaningfully verify navigation and create/edit/delete behavior; otherwise document repeatable manual device checks for those flows. Include both transaction types, Room-backed updates, confirmation cancel/confirm, errors and retry, and a fresh offline database.
- Review the completed UI against the Figma frames and `docs/DESIGN.md`. Check solid fills with no gradients, Vietnamese and English strings, locale-aware amounts and dates, accessibility labels, touch targets, keyboard/inset behavior, scrolling, and larger font settings.
- Review Clean Architecture package dependencies and data flow against `docs/ARCHITECTURE.md`, plus Material 3 use across the transaction screens. Run `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug` with the Gradle wrapper; run `:app:connectedDebugAndroidTest` when Android tests are added. Record exact commands, results, device API level, limitations, and any scope changes in this task before proposing a PR.

#### Acceptance criteria

1. Focused local tests pass for ViewModel state transitions, filtering, validation, duplicate-save prevention, failed writes, missing records, and unknown IDs; test results and any gaps are recorded here.
2. Focused Android UI tests or repeatable documented manual checks cover the offline create/edit/delete flow, confirmation cancel/confirm, loading/empty/error states, and Room-backed list updates on a named device or emulator API level.
3. Visual review against Figma and `docs/DESIGN.md` records findings for both locales, amount/date formatting, no-gradient surfaces, accessibility labels, touch targets, keyboard and system insets, scrolling, and enlarged text. Findings are fixed or documented as limitations.
4. Architecture review confirms the single `:app` module, `presentation -> domain <- data`, Material 3 use, and reuse of the existing Design System. Record the exact commands and results for `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug`, plus `:app:connectedDebugAndroidTest` if Android tests were added. Mark TASK-003 complete only after every required check has evidence.

## Verification record — 2026-10-20

- Command: `.\gradlew.bat --offline :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest` — **BUILD SUCCESSFUL**. The four requested Gradle tasks all completed. Unit suite: 13 passed, 0 failed. Android suite: 7 passed, 0 failed on Pixel_5 (AVD), API 35. Lint: 0 errors, 13 warnings (dependency version suggestions and pre-existing resources/manifest items).
- `TransactionViewModelTest` covers first observation, empty and error recovery, search/type/month, invalid and valid input, duplicate save prevention, edit with the same ID, failed write, and unknown IDs. `TransactionScreenStateTest` checks loading, empty, no-match, error, Add, and Retry UI. `TransactionFlowTest` uses an in-memory Room database through Hilt to verify expense create/edit/cancel delete/confirm delete, income creation through a suggested category, and observed list refresh.
- Device review on Pixel_5 API 35: English empty list/form and Vietnamese form were viewed at normal font size; Vietnamese list/form were viewed at 1.3× font size. Text remained readable; the form scrolled, the keyboard could be dismissed for Save, and system insets were respected. Transaction screens use the shared light theme, Material 3 controls, labeled actions, and solid fills. Vietnamese and English resource keys match. The Figma reference has only Vietnamese populated light screens, so final English, dark, and larger-screen designs remain open design decisions.
- Source review: `settings.gradle.kts` still includes only `:app`; presentation imports domain models/use cases and no Room DAO/entity/database; domain remains Android-free. Home, Statistics, Budget, Wallet, monthly totals, and account fields were not added. No commit or push was made.
- Remaining validation: the project minimum SDK is 26, but connected tests ran on API 35 only; API 26 device startup is an existing TASK-001 follow-up. The Figma icons are 1× PNG exports and may look softer on higher-density devices; vector or multi-density asset refinement can follow a later design pass.

## Screen extraction verification — 2026-10-22

- Replaced the 612-line `TransactionScreens.kt` with `TransactionListScreen.kt` (222 lines), `TransactionFormScreen.kt` (277 lines), and `TransactionDetailScreen.kt` (165 lines). Screen-specific helpers remain private; shared `ErrorBanner` and `UnavailableState` moved to `TransactionComponents.kt`.
- Consulted the Figma detail frame `3:9871` through MCP. Screen and helper bodies were preserved during extraction; only `UnavailableState` changed from private to internal so form and detail can share it. Screen signatures, navigation, state, resources, and ViewModel behavior are unchanged. No dependencies were added.
- Command: `.\gradlew.bat --offline :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest` — **BUILD SUCCESSFUL** after import cleanup. Unit suite: 13 tests, 0 failures/errors. Android suite: 7 tests, 0 failures/errors on Pixel_5 (AVD), API 35. Lint: 0 errors, 13 warnings. Existing tests cover list states and the create/edit/cancel-delete/confirm-delete flow.
- `git diff --check` passed. API 26 startup remains the existing TASK-001 follow-up.

## Shared top bar extraction verification — 2026-10-22

- Moved `TransactionTopBar` to `presentation/common/components/AppTopBar.kt` and renamed it `AppTopBar`. List, add/edit, and detail now import the common component. Added an optional `Modifier` forwarded to Material 3 `TopAppBar`; title styling, background, localized back label, back callback, and action content are preserved. The common component imports no transaction types. Updated the scope, acceptance criteria, and architecture documentation.
- Consulted the Figma Add transaction frame `3:9803` through MCP; this extraction preserves the existing rendering and behavior. No dependencies or new tests were added; existing screen-state and transaction-flow tests provide regression coverage.
- Command: `.\gradlew.bat --offline :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest` with Android Studio's bundled JBR — **BUILD SUCCESSFUL**. Unit suite: 13 tests, 0 failures/errors/skips. Android suite: 7 tests, 0 failures/errors/skips on Pixel_5 (AVD), Android 15 / API 35. Lint: 0 errors, 15 warnings concerning dependency versions, manifest label, unused colors, and launcher resources; none point to the changed Kotlin files.
- `git diff --check` passed. API 26 startup remains the existing TASK-001 follow-up.
