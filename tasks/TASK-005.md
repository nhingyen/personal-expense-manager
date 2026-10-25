# TASK-005 — Bottom Navigation & App Navigation

## Scope

Add the Figma primary navigation to the existing single-module Material 3 Compose app. Reuse Home (Dashboard), Transactions, add/edit/detail, Design System and ViewModels. The five visible entries are Home, Transactions, Add, Statistics and Settings. Add is an action opening the existing form; Statistics and Settings are localized placeholders for later tasks.

No business rules, persistence/schema, full statistics/settings features, language picker, module, or dependency changes. No commit or push.

## Implementation steps

1. Read repository/project/architecture/design guidance and current navigation/tests; consult Figma MCP for bottom-bar geometry, icons and states.
2. Centralize routes and top-level navigation with single-top, saveState/restoreState, Home as root, and destination-derived selection.
3. Add a reusable Material 3 bottom bar using existing theme and local Figma assets. Keep system insets separate; support scalable VI/EN labels and accessible controls.
4. Wire existing Home/Transactions callbacks and Add action. Prepare Statistics/Settings placeholders. Hide the bar on add, detail and edit. Preserve View all's deliberate month/filter handoff and successful write/delete return to Transactions.
5. Add focused navigation tests; run debug build, unit tests, lint and available device tests. Update current architecture/design/project documentation and record actual results here.

## Acceptance criteria

- Home is the launch/root destination. Bottom entries follow Figma order, colors, icons, selected 58 × 32 dp indicator, center 48 × 32 dp Add indicator and approximately 80 dp content height at normal font scale; system navigation insets are respected.
- Home, Transactions, Statistics and Settings derive selected semantics from the current destination. Add behaves as an action, never a persistent selected tab. Labels are localized VI/EN with at least 48 dp touch targets; large text can increase bar height.
- Statistics and Settings show only a localized title and explicit future-feature message. Existing screens are reused.
- Switching tabs preserves Home month, Transactions month/query/type and saveable screen scroll state. Reselecting a tab creates no duplicate. Navigation Compose saves/restores destination state and selected destination across Activity recreation; no new process-death guarantee for existing ViewModel form/filter data.
- Android Back from another primary destination returns to Home without cycling through prior tabs. Back/cancel from Add, detail or edit returns to its invoker. The bottom bar is hidden on those secondary screens.
- View all deliberately selects Home's month and clears list filters. Successful save/delete and unavailable-record return open Transactions even if no list was previously visited, and remove obsolete secondary destinations from Back history.
- Domain/data and financial behavior remain unchanged. No unnecessary dependencies.
- Focused tests cover selection, repeated switching/reselection, Back, Add from primary tabs, hidden bar, state restoration, and existing transaction flows. Build, Unit Test and Lint have recorded results; unavailable checks are explicitly identified.

## Testing

- Run `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
- Compile instrumentation tests with `:app:assembleDebugAndroidTest`; run `:app:connectedDebugAndroidTest` if a device/emulator is available.
- Verify Figma geometry, VI/EN labels, selected semantics, large text, insets, scroll restoration and Activity recreation through focused Compose/device tests.
- Run `git diff --check`; verify locale resource parity and that domain/data/dependencies are unchanged.

## Results

### Delivered

- Added `presentation.navigation` with shared routes/destinations, top-level navigation policy, Figma bottom bar and minimal Statistics/Settings placeholders. `ExpenseManagerApp` retains one controller and lifecycle-aware collection of the existing activity-scoped ViewModels.
- Reused Home and all transaction screens. The app host disables duplicate floating Add actions; empty-state actions and Home budget/category/notification behavior remain available. Successful saves/deletes and unavailable-record returns preserve the existing Transactions outcome.
- Consulted Figma MCP design context/screenshots for Home `3:9675`, Transactions `3:9779`, Statistics `3:10025` and Settings `3:10322`, plus Add frame metadata `3:9803`. Converted five downloaded original SVGs with Android SDK Svg2Vector into local 22 dp vectors.
- Kept the reference equal-width slots at normal text size. Measured localized labels use spare width at larger sizes; a taller wrapped row remains the fallback. Added matching VI/EN labels/messages, selected Tab semantics and a separate Add Button action.
- Added four Room-backed navigation cases to `HomeFlowTest` and one focused bar geometry/localization case in `AppBottomBarTest`. They cover repeated tab changes/reselection, filters/months, Activity recreation, list scroll restoration, Add from all primary tabs, hidden bar, secondary Back and successful-write history, 80/58/48/32/22 dp geometry, touch bounds, VI/EN and 1.3x labels.
- Updated ROADMAP, PROJECT, ARCHITECTURE and DESIGN. The former portfolio release task is now TASK-006. Domain/data/DI, Room schema, ViewModels, modules and dependency catalog are unchanged.

### Verification (2026-10-24)

| Check | Result |
| --- | --- |
| `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` | Passed on the final implementation. 47 unit tests, 0 failures/errors/skips. Lint: 0 errors, 15 existing warnings, 1 existing hint. Both app and test APKs compiled. |
| `.\gradlew.bat :app:connectedDebugAndroidTest` | Passed on Pixel_5 / Android 15 / API 35: 32 tests, 0 failures/errors/skips, including all five new navigation/bar cases and existing Home/Transaction/Room/Hilt tests. |
| Resources/source | 93 matching VI/EN keys, matching placeholders, no duplicate keys. Five non-empty local 22 dp vector assets. No domain/data/DI/dependency/schema changes. |
| Locale/visual review | The final bar test passed VI/EN geometry, selected/action semantics, touch bounds and one-line labels at 1.3x. Inspected local `app/build/task005-bottom-vi.png` and `task005-bottom-en.png`; icons remain sharp and the full English Transactions label fits. These are local verification artifacts, not production data or tracked design assets. |
| Production cold launch | Installed the final debug APK after the Gradle device run, then cold-launched MainActivity successfully on API 35. Inspected `app/build/task005-runtime.png`: Home selection, five entries, solid indicators, full English labels, existing Home content and system navigation inset render correctly. No transaction or budget data was entered for this check. |
| Whitespace | `git diff --check` passed. |

The first device run completed 32 cases with one test assertion failure: the existing form exposes a formatted `123 ₫` value while the new assertion expected raw `123`. The assertion now checks the rendered numeric content and the ViewModel's exact raw input independently. That failed run is not counted as a pass. Initial screenshot review also found the English Transactions label wrapping to a lone final letter at 1.3x; adaptive slot widths and a one-line localized-label assertion address this before the final rerun.

### Remaining limitations

- Statistics and Settings are intentional placeholders; full feature work remains outside TASK-005.
- API 26 startup and a complete spoken TalkBack traversal remain unverified, carrying forward the existing project limitations. Device results apply to Pixel_5 / Android 15 / API 35.
- Navigation Compose saves/restores destination and saveable screen state. Existing transaction form/filter data is retained across tab changes and Activity recreation by the existing ViewModel; this task does not add persistence of that data across process death.
- No commit, push, staging or Git-history changes were performed.
