# TASK-008 — Monthly Budget Management

## Scope

Complete monthly budget management from Home/Dashboard by extending the existing TASK-004 sheet and HomeViewModel. Reuse MonthlyBudget, observe/save/calculate use cases, MonthlyBudgetRepository and its SharedPreferences implementation; transaction spending continues to come from the existing Room observation. No additional repository, ViewModel, module, dependency or schema is needed.

- Select a month directly in the budget sheet; set, edit or remove its positive whole-VND limit.
- Show saved limit, actual expense total, exact remaining amount, percentage and progress; expose reached/exceeded warnings in both Dashboard and the sheet.
- Keep month, transaction snapshot and budget limit consistent; reset drafts when changing months and block duplicate writes/month changes while saving.
- Handle unset, zero spending, reached/exceeded, load/write errors, stale transactions, arithmetic overflow and reactive transaction changes with matching VI/EN resources.
- Update README to reflect the implemented behavior and AGENTS to require README updates after feature tasks.
- Refresh README screenshots from the running app for the changed Home/budget UI, including unset and over-budget states.

Figma MCP reference: Budgets `3:10148`, monthly overview `3:10166`, month selector `3:10160`. Adapt the monthly overview to the existing Material 3 sheet using shared theme/components. Category budgets, Wallet, Settings, rollover, recurring budgets, OS notifications and unrelated refactoring are excluded.

## Acceptance criteria

1. Home's Budget shortcut/card opens the existing sheet, with month controls and saved limit/expense/remaining overview. Switching months updates Dashboard and editor together without starting additional collectors.
2. Saving creates or replaces only the selected month's limit and persists through the existing preference store. Removing a limit leaves transactions and other months unchanged. Dismissal/cancel without saving makes no write.
3. Empty, zero, negative, fractional, non-numeric and Long-overflow input is rejected. Write failure retains the draft; retry uses the relevant Save/Remove action. Duplicate saves and month changes during a write cannot affect another month.
4. Unset budget shows setup copy without invented percentage or progress. A configured month without expenses shows zero use and the full remaining amount.
5. Only selected-month expenses consume the limit. Exact-limit and over-limit states show textual warnings; over-limit remaining stays negative, percentage can exceed 100%, and only the visual bar is capped.
6. Adding/editing/deleting transactions, changing their type/date or changing months recalculates from the existing snapshot. Retry/error/overflow states never present stale data as fresh or substitute a zero total.
7. Unit tests cover calculation boundaries and ViewModel month/draft/write/observation behavior. Focused device checks exercise the monthly sheet and existing persistence flow; Build, Unit Test and Lint are run and recorded accurately.
8. README and related task/design/architecture documentation match the delivered scope. No commit or push.
9. README displays current full-device screenshots for Home and the expanded budget sheet, plus unset/over-budget examples. Captures use fictional data isolated from existing user data; local image links resolve and PNGs decode at the emulator's native resolution.

## Testing

- Extend MonthlyBudgetUseCasesTest for selected-month expense integration, income exclusion, exact/over limit and Long boundaries; retain existing unset/zero/validation tests.
- Extend HomeBudgetTest for edit/replace/remove isolation, fresh drafts across months, delayed observation, save locking, failures/retry, restoration and reactive transaction changes.
- Extend HomeFlowTest for sheet month selection, overview, edit/cancel and Dashboard synchronization with Room. Keep existing budget persistence tests.
- Run `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
- Run focused connected tests when the available emulator permits; report actual results and limitations.
- Check localization keys/placeholders and `git diff --check`.

## Results

Completed on 2026-10-26.

- Audited and reused the existing MonthlyBudget model/use cases, preference repository/Hilt wiring, HomeViewModel StateFlow and Room transaction snapshot. No new storage, schema, dependency, module or parallel budget implementation.
- Extended the existing Home sheet with month controls and the Figma monthly overview. Dashboard and sheet share percentage/progress/warning rendering; remaining amounts retain their exact sign, and reached/exceeded warnings are explicit text. Unset and no-expense states stay distinct.
- Added month-specific draft reset, untouched-draft prefill on delayed observation, pending-write guards for month/input/dismissal, and cache invalidation on budget reload/error. Failed Save/Remove actions retain input and use their original action controls for retry.
- Added 2 domain calculation tests, 7 HomeViewModel budget tests, 2 budget screen tests and 1 Room-backed Home flow. Tests cover independent months, edit/remove/cancel, year-boundary restoration, delayed data, duplicate writes, errors/retry, actual transaction changes, zero/reached/exceeded budgets and Long boundaries.
- Updated README, AGENTS, ROADMAP and the directly related PROJECT/ARCHITECTURE/DESIGN notes. Refreshed the gallery's Home and Monthly budget images and added unset/over-budget examples during the screenshot follow-up below. No Wallet, Settings, category budgets, commit or push.

### Verification

Final command:

```powershell
.\gradlew.bat --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest :app:connectedDebugAndroidTest '-Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true'
```

| Check | Result |
| --- | --- |
| Debug build and instrumented APK | Passed. Final full run: BUILD SUCCESSFUL. |
| Unit tests | 78 tests, 0 failures/errors/skips, including all 9 new domain/ViewModel cases. Executed on the final application/unit-test sources in the preceding fresh-daemon run; up to date in the final full-device run after only locale-fixture changes to instrumented tests. |
| Lint | Passed: 0 errors, 15 existing warnings and 1 existing hint. No new budget issue; no checks were disabled or suppressed. |
| Connected tests | Full suite passed on Pixel_5 / Android 15 / API 35: 41 tests, 0 failures/errors/skips. Includes 16 Home flow tests, 9 Home screen tests, 2 new budget screen tests, both preference persistence tests, and existing Room/Hilt/transaction/navigation/Statistics cases. |
| Localization and documentation | 120 matching VI/EN resource keys and placeholders, no duplicate keys; all 33 local links in modified documentation resolve. |
| Visual review | Inspected `app/build/task008-budget-vi.png` and `task008-budget-en.png`, captured from the sheet on the emulator. Both resource labels and month/currency formatting match the selected locale; the overview follows the Figma pastel card, total, progress and spent/remaining slots. The calendar/chevron callsites reuse the existing non-empty local vectors at 18/20 dp in 48 dp controls. Long.MAX_VALUE spending and 1.5x font scale pass the screen test with exact amounts and a reachable Save action. Images/sample values are test artifacts, not runtime seed data. |
| Whitespace | `git diff --check` passed. |

An initial incremental Lint run stalled in the analyzer's PSI tree walk and was interrupted. A fresh single-use Gradle daemon completed Build, Unit Test and Lint successfully. The first filtered device invocation executed only the 2 new screen cases. Visual inspection then identified that the test fixture's LocalContext override did not change stringResource labels on the English emulator; the fixture now explicitly supplies LocalResources and asserts translated labels. The existing Home reference fixture uses the same explicit resource override. The final unfiltered device run above passed all 41 tests. These preliminary checks are separate from final verification.

### Verification limits

Device coverage is API 35; API 26 startup and a complete spoken TalkBack traversal were not rerun. Saved month restoration is covered by a restored SavedStateHandle and Activity recreation; a system process-kill scenario was not run. Unsaved drafts do not survive process death. Home and budget documentation captures were refreshed in the follow-up below; the other six primary gallery images remain from the earlier running app. Category budgets, automatic rollover, Wallet, Settings and OS notification delivery remain outside this task.

### README screenshot follow-up (2026-10-26)

At the user's request, captured the current debug app through ADB `screencap` on Pixel_5 / Android 15 / API 35, in English (en-US), at normal font scale and 1080 × 2340 px. Replaced `docs/screenshots/home.png` and `monthly-budget.png`; added `monthly-budget-unset.png` and `monthly-budget-exceeded.png` and linked both from README. These are unmodified full-device PNG captures, including system bars and the actual Material 3 sheet; they are not cropped Compose test images.

Used a temporary secondary Android user with fictional Room records: October income 17,500,000 VND, expenses 3,420,000 VND, initial limit 5,000,000 VND. Selected November in the sheet to capture an unset budget with zero spending. Edited and saved October's limit to 2,500,000 VND through the app, then captured 136% used, remaining -920,000 VND and the explicit exceedance warning. No runtime sample data or app code was added. Returned the emulator to Owner and removed the temporary user and device capture files after review; existing owner data was not edited.

Visually reviewed all four changed/new images, checked PNG decoding/native dimensions and README image links, and ran `git diff --check`. This follow-up changes documentation and screenshot assets only; Build, Unit Test and Lint were not rerun, and the feature verification above remains the actual result of those checks.

## Task ID history

The user reassigned TASK-008 to Monthly Budget Management on 2026-10-26. Its previous documentation-only scope produced the existing English README and eight emulator screenshots under `docs/screenshots/`. Six gallery images remain historical captures; Home and Monthly budget were refreshed for this feature, with two additional budget states. That earlier task recorded 69 passing unit tests and lint with 0 errors, 15 warnings and 1 hint; these are historical results, not verification of this feature. The broader portfolio release pass remains open.
