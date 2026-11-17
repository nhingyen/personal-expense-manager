# TASK-017 — Category-based Budgets

## Outcome

Add offline monthly limits per expense category while preserving the existing whole-month budget preferences and the Room v3 category/transaction compatibility boundary. Delivery is split so the Room/domain foundation is reviewed before any UI work.

## Approved scope

### Phase 1 — storage and domain

- Add pure Kotlin category-budget models, repository contracts and use cases.
- Calculate selected-month expense spending by stable category ID while matching historical transaction strings with the existing Unicode-aware normalization.
- Detect exact `Long` overflow and reject inconsistent category/transaction snapshots instead of publishing misleading zero spending.
- Add an additive Room v4 `category_budgets` table keyed by category ID, year and month.
- Validate writes transactionally: limits are positive and belong to active expense categories. Archived limits remain readable and removable.
- Keep rename stable through category IDs; retain archived limits and historical transaction strings.
- Guard hard category deletion while a category budget references it.
- Add and register non-destructive `MIGRATION_3_4`, retain 1 → 2 → 3 → 4 upgrades and export schema v4.
- Add focused JVM, DAO/repository and migration tests.

Phase 1 excludes Compose UI, ViewModel integration, Figma work, new dependencies, unrelated refactors, network behavior, OS notifications, commit, push, merge, tag and release.

### Phase 2 — presentation

- Extend the existing Home budget sheet without replacing its overall monthly budget.
- Show configured category limit, spent, remaining, progress and reached/exceeded warnings.
- Add, edit and remove limits for active expense categories. Archived configured categories remain read-only with Remove available.
- Reuse HomeViewModel's transaction snapshot and observe the Room category catalog/category limits reactively.
- Retain the last valid category calculation across inconsistent Room Flow emissions and expose persistent load, consistency and overflow errors with Retry.
- Share selected-month and pending-write locking behavior with the existing overall budget editor.
- Add category warnings to the existing in-app notification sheet; no OS notifications.
- Add matching VI/EN resources and focused ViewModel, Compose and Room-backed integration tests.

Budgets `3:10148` was consulted through Figma MCP design context and screenshot before implementation. The existing sheet adapts its compact category cards, 34 dp category icons, 16 dp corners, 10 dp padding and 7 dp progress while retaining current Material 3 components, scrolling and navigation. No new asset is required.

## Acceptance criteria

1. One positive whole-VND limit can be created or replaced per expense category and calendar month and can be removed without changing transactions or other limits.
2. Only selected-month expenses contribute to category spending. Income, adjacent months and wallet metadata do not contribute.
3. Spending groups legacy case/whitespace variants through `trim().lowercase(Locale.ROOT)` and category IDs; category rename does not move or delete a limit.
4. Spending sum overflow throws explicitly. Missing/duplicate catalog mappings throw an inconsistent-snapshot error rather than producing a fresh zero.
5. New or changed limits require an existing active `EXPENSE` category. Income, archived and missing categories are rejected atomically; archived existing limits remain observable and removable.
6. Hard category deletion returns `IN_USE` while either historical transactions or a category budget references the category.
7. Room v3 → v4 creates only the category-budget table and index. Existing transactions, wallets, categories, IDs, strings and sequences remain unchanged; the 1 → 2 → 3 → 4 chain validates.
8. Existing `monthly_budgets` SharedPreferences name, keys and values are not migrated, rewritten or read by category-budget storage.
9. Focused JVM calculation/use-case tests, Room DAO/repository tests and migration tests pass. Verification results are recorded without claiming unrun checks.
10. No UI, Figma call, dependency, unrelated refactor or Git-history operation is included in Phase 1.
11. The existing Home budget sheet keeps its overall monthly overview/editor and adds configured category-budget cards for the same selected month.
12. Active expense categories can add/edit/remove limits. Income categories are absent; archived configured categories are read-only except for Remove.
13. Category cards and in-app notices show exact spent/limit/remaining values, capped visual progress and textual reached/exceeded warnings in VI/EN.
14. A pending overall or category budget write blocks month, draft, dismissal and competing budget actions. Failures retain the relevant draft and allow retry.
15. Inconsistent category/transaction emissions never replace the last valid rows with zero. A persistent consistency/load/overflow error is visible and retryable; a later coherent emission recovers automatically.
16. Existing Home navigation, month restoration, transaction updates and whole-month budget behavior remain unchanged.

## Verification to record

- Focused JVM `CategoryBudgetUseCasesTest`.
- Focused instrumented `RoomCategoryBudgetRepositoryTest`.
- Room migration tests for v3 → v4 and chained v1 → v2 → v3 → v4.
- Debug Kotlin/Room schema compilation and exported schema v4.
- Relevant category regression tests and `git diff --check`.

## Phase 1 results — 2026-11-14

- Confirmed TASK-016 is merged at the current `develop` HEAD before editing. The only pre-existing worktree item was an unrelated untracked `.claude/` directory, which remains untouched.
- Added pure category-budget models, repository/use-case contracts and exact selected-month expense calculation. Legacy category strings use the TASK-016 normalization rule; inconsistent snapshots and `Long` overflow fail explicitly.
- Added Room v4 category-budget persistence keyed by stable category ID/year/month, transactional active-expense validation, archived-limit removal, category deletion guarding, Hilt wiring, additive `MIGRATION_3_4` and exported schema 4.
- Existing whole-month `monthly_budgets` preferences and transaction/category strings are unchanged. No UI, Figma call, dependency or Git-history operation was included.

### Verification

| Check | Result |
| --- | --- |
| Focused JVM tests | PASS: `CategoryBudgetUseCasesTest`, 5 tests, 0 failures/errors/skips. |
| Debug and Android-test compilation | PASS: `compileDebugKotlin` and `compileDebugAndroidTestKotlin`; Room generated and validated schema 4. |
| Category-budget repository tests | PASS on Pixel 5 / API 35: 3 tests, including replace/remove, direct positive-limit validation, active expense validation, rename/archive retention and guarded delete. |
| Category-budget migration tests | PASS on Pixel 5 / API 35: 2 tests covering v3 → v4 and v1 → v2 → v3 → v4, row/sequence preservation, foreign keys and a post-upgrade budget write. |
| Category repository regression | PASS on Pixel 5 / API 35: 4 tests, 0 failures/errors/skips. |
| Diff whitespace | `git diff --check` PASS. |

The initial Gradle invocation could not create its configured `C:\.gradle` wrapper lock inside the filesystem sandbox and did not reach the build. The same focused command was rerun with approved cache access and passed; this was not a test failure. Full unit/instrumented suites, lint, Release build and API 26 migration were not run for this Phase 1 verification.

## Phase 2 results — 2026-11-14

- Extended the existing Home budget sheet and notification panel with reactive category-budget cards and an inline add/edit editor. The overall monthly budget and navigation remain in place.
- HomeViewModel combines catalog and limit observations with its existing transaction snapshot. Only a fully coherent calculation replaces cached rows; inconsistency or overflow retains the last valid rows, marks them stale and exposes Retry.
- Active expense categories are eligible for add/edit. Archived configured budgets remain visible and removable, and an editor closes if its category is archived externally. One pending budget write locks both monthly and category budget interactions.
- Reused local category icons, theme colors, Material 3 controls and the Figma card geometry. Added no assets, dependencies, migration or route.
- The monthly editor's input, save and clear controls also honor the shared write lock, so a category-budget write cannot race through the existing whole-month editor.

### Verification

| Check | Result |
| --- | --- |
| Production/JVM/Android-test compilation | PASS: `compileDebugKotlin`, `compileDebugUnitTestKotlin`, and `compileDebugAndroidTestKotlin`. |
| Focused Home JVM tests | PASS: 28 tests, 0 failures/errors/skips (`HomeBudgetTest` 16, `HomeObservationTest` 9, `HomeViewModelTest` 3). |
| Budget Compose tests | PASS on Pixel 5 / API 35: `MonthlyBudgetScreenTest`, 4 tests, including exact cards/warnings, archived actions, editor choices, VI/EN and existing monthly states. |
| Home integration tests | PASS on Pixel 5 / API 35: `HomeFlowTest`, 17 tests, including Room persistence/reactivity/archive/remove and existing navigation/recreation/monthly-budget flows. |
| Lint | PASS: `:app:lintDebug`. |
| Final focused UI rerun and diff hygiene | PASS: `MonthlyBudgetScreenTest`, 4 tests, 0 failures/errors/skips after aligning monthly editor controls with the shared write lock; `git diff --check` PASS. |

One preliminary compile failed on an invalid direct Compose `weight` import; removing the import resolved it. The first Compose run then had one test assertion that expected merged descendant text on an unmerged card; the UI hierarchy was correct, the assertion was corrected to inspect its text descendants, and the complete 4-test class passed. These preliminary runs are not reported as passing verification.
