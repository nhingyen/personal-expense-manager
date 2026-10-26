# TASK-007 — Statistics & Charts

## Scope

Replace the Statistics tab placeholder with a Room-backed, read-only Statistics screen. Follow Figma Statistics `3:9943`, the existing Design System, Navigation and VI/EN resources. Reuse `ObserveTransactionsUseCase`, `TransactionRepository` and `CalculateMonthlySummaryUseCase`; add pure statistics calculation and a dedicated MVVM/StateFlow ViewModel.

Show selected-month income/expense, expense distribution by saved category with amounts and localized percentages, and paired income/expense bars for the six calendar months ending in the selected month. This monthly comparison intentionally replaces Figma's weekly expense chart to satisfy this task. Use Compose Canvas/layout primitives without a chart dependency. Omit the export control because exporting is outside scope. Dynamic charts must use actual records, never the reference SVG slices/sample amounts.

No Budget, Wallet, Settings, export, schema changes, new modules or dependencies. Do not commit/push.

## Acceptance criteria

- Statistics opens from the existing bottom bar and keeps its selection/Back/Add behavior. Reuse the shared top bar, month selector, palette, typography and local direction/navigation icons.
- Previous/next month updates all displayed values together; selected month survives Activity/process recreation through SavedStateHandle and stays independent of Home/list filters.
- Monthly totals are exact whole-VND Long values. Categories include expenses only, group by the stored category string and sort by descending amount then name; all categories remain readable without truncation. Percentage is category expense / total expense, localized with up to one decimal place; independently rounded percentages may sum to 99.9% or 100.1%.
- Comparison has six chronological calendar months, including zero months and year boundaries. Both series use the same maximum scale and expose month, type and exact VND amounts as accessible text. The range is explicitly labeled.
- Empty month shows zero totals and an explicit empty message; income-only months show a no-expenses message and no misleading donut. Empty comparison shows a separate message. Zero denominators never yield NaN/Infinity or a fake positive bar.
- One cancellable observer consumes repository snapshots. Adds/edits/deletes refresh all statistics. Month changes reuse the snapshot without adding collectors. Loading/error/retry are distinct; failed reads hide financial data until a fresh snapshot rather than silently displaying stale values. Overflow reports a localized calculation error without wrapped totals, and later valid data recovers.
- UI is scrollable, respects insets/font scaling, supports VI/EN, and provides text equivalents for chart colors. No runtime Figma asset URLs.
- Focused unit tests cover calculation and ViewModel behavior, including boundaries, empty/zero cases, updates, saved month, errors/retry/cancellation and overflow. Build, Unit Test and Lint are run and their actual outcomes recorded.

## Testing

- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest`
- Run focused Compose/device checks when an emulator is available: empty/income-only/populated, month controls, reactive Room updates, tab/recreation, VI/EN and large text. Record checks that could not run.
- `git diff --check`; inspect matching localized resource keys/placeholders, domain dependency boundaries and scope.

## Results

### Delivered

- Consulted Figma MCP Statistics `3:9943` design context and screenshot. Reused existing theme, top bar, month selector, summary direction vectors, error banner and bottom navigation. No static reference chart SVGs or sample amounts are used at runtime.
- Added pure `MonthlyStatistics`/`CategoryExpense` and `CalculateMonthlyStatisticsUseCase`, reusing checked monthly summary calculations. All six comparison months and the selected-month category distribution derive from the same repository snapshot.
- Added Hilt-wired `StatisticsViewModel` with immutable StateFlow, one cancellable transaction observer, independent saved month, snapshot-based month changes, loading/read-error/retry, checked overflow and recovery. Existing Room schema, DAO, repository contract and write paths are unchanged.
- Replaced the Statistics route placeholder. Show exact totals, dynamic expense donut, localized percentages, all saved categories and paired monthly income/expense bars with exact text equivalents. Six groups fit at normal text size; narrow screens/larger fonts can scroll the plot. Summary cards and donut/legend adapt to available space. Long amounts are kept in the exact summary/legend; the compact center amount is omitted when it cannot fit inside the donut.
- Added 6 calculation and 8 ViewModel unit tests, 2 Compose screen cases, and 1 Room-backed navigation/update case. Updated the existing navigation assertion to expect the real Statistics screen.
- Updated ROADMAP, PROJECT, ARCHITECTURE and DESIGN. No module, dependency, Budget, Wallet, Settings or export feature was added. No staging, commit, push or history changes.

### Verification (2026-10-26)

Final command:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest '-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true'
```

| Check | Result |
| --- | --- |
| Debug build and instrumentation APK | Passed on the final implementation. |
| Unit tests | 61 tests, 0 failures/errors/skips, including all 14 new calculation/ViewModel cases. |
| Lint | Passed: 0 errors, 15 existing warnings and 1 existing hint. No Statistics source issue is reported. |
| Connected tests | Passed on Pixel_5 / Android 15 / API 35: 35 tests, 0 failures/errors/skips, including the 3 new Statistics cases and existing navigation/transaction/Room/Hilt checks. |
| Locale/source checks | 106 matching VI/EN keys, matching format placeholders and no duplicate keys. Domain additions have no Android/Compose/Room/Hilt imports; no dependency, module or schema changes. |
| Visual review | Inspected final local `app/build/task007-statistics-vi.png`, `task007-statistics-en.png` and `task007-comparison-en.png`. Totals, donut proportions/legend and all six monthly bar groups render with the existing solid palette; the final month's paired bars are fully visible at normal text size. The Compose cases also pass at 1.5x with a long saved category and Long.MAX_VALUE expense. Screenshots are local ignored build artifacts; sample records exist only in tests. |
| Whitespace | `git diff --check` passed. |

The first device run had 34 passes and one failed screen-test assertion: after scrolling to the comparison and switching locales, a LazyColumn item was no longer composed. The test now scrolls to the top before looking up that item. A subsequent full run passed 35/35. Visual review then found the last bar group partially offscreen at normal text size; responsive group widths were corrected and the checks rerun. These intermediate runs are distinct from the final verification.

### Remaining limitations

- Device coverage is Pixel_5 / Android 15 / API 35. API 26 startup and a complete spoken TalkBack traversal remain unverified. SavedStateHandle restoration is covered with a restored handle in unit tests and Activity recreation on-device; a system-initiated process-kill scenario was not run.
- Category names are user data and remain stored verbatim across language changes. Percentages round independently to one decimal place; compact donut-center amounts may round, while summary/category/comparison text preserves exact whole VND.
- The six-month comparison and omitted export button intentionally adapt the Figma reference to this task. Dark appearance and full management/export features remain outside scope.
