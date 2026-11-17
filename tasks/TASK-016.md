# TASK-016 — Custom Categories

## Outcome

Add an offline category catalog without replacing the transaction category string or changing historical financial meaning. Delivery is split so the Room/domain foundation can be reviewed before any UI work.

## Approved scope

### Phase 1 — storage and domain

- Keep `Transaction.category` and `transactions.category` as `String`.
- Add Room schema v3 with a `categories` table, DAO, repository, pure domain model/use cases, Hilt wiring, and an exported schema.
- Add a non-destructive `MIGRATION_2_3` and retain the complete `1 -> 2 -> 3` upgrade path.
- Populate the catalog from distinct existing transaction type/category values without deleting or rewriting transaction rows.
- Compare trimmed names case-insensitively with `Locale.ROOT`; one normalized name may exist per transaction type.
- Rename a category and every matching transaction category atomically, including legacy case variants that normalize to the same name.
- Archive without changing historical transactions. Hard delete only when no historical transaction matches the category.
- Treat free-text input matching an archived category as an explicit archived conflict: do not create a duplicate and do not restore it implicitly.
- Add focused domain, repository, and migration tests.

### Phase 2 — category UI and transaction integration

- Add a separate Settings entry and category-management screen for list, create, rename, archive, restore, and guarded delete.
- Keep the existing Home category shortcut as a quick chooser that prefills Add; do not turn it into management navigation.
- Combine active catalog values with localized suggestions in Add/Edit. Hide archived values from selection.
- Register new free text and save the transaction in one Room transaction. Reject an archived normalized match without restoring it or partially writing a transaction.
- Allow an unchanged archived category on an existing historical transaction while rejecting assignment of that archived category to another transaction.
- Add focused ViewModel, Compose, repository-integration, navigation-regression, and VI/EN resource coverage.

No network, additional module, dependency, destructive migration, transaction foreign key/category ID, unrelated refactor, commit, push, or merge is included.

## Acceptance criteria

1. Fresh databases and migrated v2 databases expose reactive categories grouped by income/expense.
2. Migration preserves every transaction ID, type, amount, category string, date, note, wallet reference, ordering inputs, and autoincrement behavior.
3. Names are trimmed, nonblank, at most 60 Unicode code points, and duplicate checks are case-insensitive using `Locale.ROOT` within a type; the same name may exist for the other type.
4. Rename and all matching transaction string updates commit or roll back together.
5. Archived categories remain attached to historical transaction strings and remain available to transaction-derived filters/statistics; an archived free-text match reports a conflict until restored or renamed.
6. Delete reports `IN_USE` when any matching transaction exists and otherwise removes only the catalog row.
7. Room validates schema v3 and focused tests cover create/duplicate/archive/restore/rename/delete, Unicode normalization behavior, v2 -> v3 migration, and chained v1 -> v2 -> v3 migration.
8. Production database construction registers both migrations and never enables destructive fallback.
9. Category management is reachable independently from Settings while the Home category shortcut still opens its existing prefilled Add flow.
10. Add/Edit suggestions contain localized defaults plus active catalog values without normalized duplicates; archived catalog values are hidden.
11. Creating a category through transaction free text and saving the transaction either both commit or both roll back. An archived match produces a category field error and no navigation.
12. Existing transactions, filters, statistics, Home totals, wallet behavior, and CSV export continue consuming the stored transaction category string.

## Verification to record

- Focused JVM category use-case tests.
- Focused Room repository tests.
- Room migration tests for v2 -> v3 and 1 -> 2 -> 3 when an emulator/device is available.
- Relevant compile/schema export check.

## Phase 1 verification — 2026-11-13

- `:app:testDebugUnitTest --tests "com.ngyennhi.expensemanager.domain.usecase.CategoryUseCasesTest"` — PASS, 4 focused JVM tests.
- `:app:connectedDebugAndroidTest` filtered to `RoomCategoryRepositoryTest` — PASS on Pixel 5 / API 35, 4 tests.
- `:app:connectedDebugAndroidTest` filtered to `CategoryMigrationTest` — PASS on Pixel 5 / API 35, 2 tests covering v2 -> v3 and v1 -> v2 -> v3.
- `:app:connectedDebugAndroidTest` filtered to `WalletMigrationTest` — PASS on Pixel 5 / API 35, 4 existing migration/persistence regression tests after registering v3.
- Debug compilation/KSP exported and Room validated schema v3. `git diff --check` is recorded at handoff.
- UI, full regression, lint, Release build and API 26 migration remain outside this Phase 1 verification.

## Phase 2 verification — 2026-11-13

- `:app:compileDebugKotlin` and `:app:compileDebugAndroidTestKotlin` — PASS.
- Focused JVM tests filtered to `CategoryUseCasesTest`, `CategoryViewModelTest`, and `TransactionViewModelTest` — PASS, 23 tests on the final reviewed source.
- `RoomTransactionRepositoryTest` — PASS on Pixel 5 / API 35, 3 tests including atomic registration/save, archived rejection, and unchanged historical editing.
- `CategoryManagementScreenTest` — PASS on Pixel 5 / API 35, 3 tests covering management actions, duplicate/delete messaging, and active-only form choices.
- `SettingsScreenTest` — PASS on Pixel 5 / API 35, 3 tests.
- `HomeFlowTest` — PASS on Pixel 5 / API 35, 16 tests including the existing category shortcut.
- `TransactionFlowTest` — PASS on Pixel 5 / API 35, 2 transaction persistence/navigation regressions.
- Post-review archived legacy regression — PASS on Pixel 5 / API 35: editing only amount/note retained the migrated `fOoD` transaction string while the catalog's archived canonical spelling remained `Food`. The existing atomic registration/archived-conflict repository test also passed on the final reviewed source.
- Full unit/instrumented suites, lint, Release build, export-specific UI regression, and API 26 remain outside this focused Phase 2 verification.
