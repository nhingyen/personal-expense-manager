# TASK-006 — Transaction Search & Filter

## Scope

Extend the existing Transactions screen, MVVM StateFlow and Room observation with category and inclusive date-range filtering. Reuse existing search, type chips, month selector, rows and Design System. Extract matching into a pure Kotlin use case; keep one repository observer and the unchanged Room schema. No dependency/module changes, Statistics, Budget or Wallet implementation. No commit or push.

## Acceptance criteria

- Search matches a substring of saved note OR category, ignores case and surrounding query whitespace, and handles blank queries/null notes. Vietnamese accents remain significant; saved user text is not translated.
- Type, exact category and time filters combine with search using AND. Category options come from all saved records, including custom categories, independently of the current results; a selected category remains visible if its last record disappears.
- The initial period is the selected month. A custom inclusive start/end range replaces that month and supports crossing months/years and a single day. Invalid/incomplete ranges cannot be applied. Returning to monthly mode restores the selected month.
- The existing Figma filter icon opens a working category/period editor. Apply publishes both choices together; Cancel/Back discards drafts. Active category/range and reset are visible. Reset clears query/type/category/range and selects the current month.
- Loading, errors/stale data and observed empty results remain distinct. No matches show a reset action; an unfiltered empty month shows Add.
- Room inserts/edits/deletes refresh matching results with filters retained and without changing stored records. Home stays independent. Tab changes and Activity recreation retain committed filters through the existing ViewModel. View all clears every filter and selects Home's month. Process-death persistence is outside scope.
- User-facing labels, date formatting and accessibility descriptions support VI/EN, existing theme/controls and minimum touch bounds. Consult Figma MCP Transactions `3:9699`; adapt category/date editor with Material 3 because no editor frame is documented.
- Focused domain, ViewModel and UI/Room tests cover matching, combination, date boundaries, reset, no matches and reactive changes. Build, Unit Test and Lint results are recorded below.

## Testing

- Run `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest`.
- Run available emulator tests with `:app:connectedDebugAndroidTest`, including category/period editor apply/cancel, date selection, combined matching and reset.
- Verify VI/EN resource key/placeholder parity and `git diff --check`.
- Record actual outcomes and any remaining device/accessibility limitations here after implementation.

## Results

### Delivered

- Extracted matching from UI state into pure `FilterTransactionsUseCase` and added validated inclusive `TransactionDateRange`. The existing ViewModel publishes matching snapshots from one unchanged Room repository observation; filtering retains date/id order and never writes records.
- Reused Figma Transactions `3:9699` through MCP design context and screenshot, including existing search, type chips, filter icon, month selector and transaction rows. The icon opens a Material 3 category/period sheet with saveable drafts, Apply/Cancel/Reset, active selections and a reset action in no-match results.
- Reused the form's date picker through extracted `TransactionDatePicker` for From/To. Independent dates allow a single day, unlike the range picker's strict start/end validation in text-entry mode. Reversed dates show an accessible error and disable Apply; empty date input cannot be confirmed. No duplicate picker conversion logic remains in the form.
- Added VI/EN resources and accessible search/selection/error semantics; type chips grow for scaled/long labels, and filter-sheet buttons have at least 48 dp height. Updated View all to clear every filter, while tab changes/recreation and observation retry retain committed filters.
- Added six domain tests, two ViewModel tests, a Room-backed combined-filter UI case and two actual VI/EN date-editor cases at 1.3x system font scale. Existing navigation cases now also verify category/range preservation and View all clearing. Locale-editor tests use Android 13+ per-app language APIs and restore the previous emulator locale/font scale.
- Updated ROADMAP (portfolio release moves to TASK-007), PROJECT, ARCHITECTURE and DESIGN. No Statistics, Budget, Wallet, module, schema, repository API or dependency changes.

### Verification

Final verification on 2026-10-25:

| Check | Result |
| --- | --- |
| `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest` | Passed on the final code after a cold emulator restart. App and test APKs built; 55 unit tests and 35 device tests, 0 failures/errors/skips. |
| Lint | 0 errors, 15 existing warnings, 1 existing hint. No new lint findings. |
| Device coverage | Pixel_5 / Android 15 / API 35. Includes combined filters with real Room insert/update/delete, category cancellation, reset from no matches, tab/recreation retention, View all clearing, and actual VI/EN editor validation at 1.3x system font scale. Reversed dates are blocked, single-day dates are accepted, and dates across months apply together with category. |
| Localization/visual review | 104 matching VI/EN keys and placeholders, no duplicates. Inspected local `app/build/task006-filters-en.png`, `app/build/task006-filters-vi.png` and the Vietnamese date-input screenshot `app/build/task006-debug.png`. Labels and dates remain readable at 1.3x; controls farther down the sheet are reached by scrolling. These are ignored local test artifacts. System font scale was restored to 1.0 after the run. |
| Repository review | `git diff --check` passed. Data implementation, Room schema, dependency catalog and module/build configuration are unchanged. No commit, push or staging. |

Earlier device runs are not counted as passes: they exposed control-height and asynchronous locale/font test-configuration issues, which were corrected. A later retry stalled with emulator Binder `No space left on device` errors; the emulator was cold-restarted without wiping its data before the successful final full run.

### Remaining limitations

- Results apply to Pixel_5 / Android 15 / API 35. API 26 startup and a complete spoken TalkBack traversal remain unverified, carrying forward the project limitations.
- Committed filters survive tabs and Activity recreation through the existing ViewModel. Filter persistence after process death is outside this task.
- Search preserves Vietnamese accents and compares saved category/note text; it does not translate user-entered data between locales.
- No commit, push, staging or Git-history changes were performed.
