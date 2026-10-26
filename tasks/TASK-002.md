# TASK-002: Transaction Domain & Room Persistence

**Status:** Implemented and verified on API 35. The separate API 26 startup check for TASK-001 remains open; TASK-002 was implemented at the user's request.

**Outcome:** Store income and expense records on the device and expose them through a domain repository. This task provides the data foundation; record screens and monthly totals belong to TASK-003 and TASK-004.

## Scope

- Add the pure Kotlin `Transaction` model, `TransactionType`, `TransactionRepository` interface, and the business rules needed for valid records under `domain`.
- Add `AddTransactionUseCase`, `UpdateTransactionUseCase`, `DeleteTransactionUseCase`, and `ObserveTransactionsUseCase` under `domain`.
- Add `TransactionEntity`, `TransactionDao`, `ExpenseManagerDatabase`, domain/entity mappers, and `RoomTransactionRepository` under `data`. Observe records with a Room-backed `Flow` from the DAO.
- Use Hilt modules under `di` to provide the database and DAO, bind `RoomTransactionRepository` to `TransactionRepository`, and provide the four use cases without Hilt annotations in `domain`.
- Keep the existing single `:app` module. Add a dependency through the version catalog only if the storage work requires it.

## Acceptance criteria

1. The domain defines `TransactionType` with exactly `INCOME` and `EXPENSE`. A `Transaction` has a stable `Long` ID, a type, an `amountVnd: Long` in whole VND, a nonblank category, a `LocalDate`, and an optional note. No floating-point amount is used. Records of both types retain every field through a storage round trip.
2. `TransactionRepository` is an interface in `domain` with a `Flow<List<Transaction>>` observation operation and suspend create, update, and delete operations. Create accepts a `TransactionDraft` without an ID and returns the generated `Long` ID; update and delete return `true` for an existing ID and `false` for an unknown ID. An unknown ID leaves stored records unchanged; tests cover both operations.
3. A new database emits an empty list through the repository; successful create, update, and delete operations emit the resulting list. Updating an existing ID changes its fields without creating a second record. Records are ordered by date descending, then ID descending when dates match.
4. `AddTransactionUseCase` and `UpdateTransactionUseCase` accept only `amountVnd > 0` and a nonblank category; zero or negative amounts are rejected before calling the repository. `DeleteTransactionUseCase` forwards the target ID and returns the repository's success or unknown-ID result. `ObserveTransactionsUseCase` exposes the repository's ordered `Flow<List<Transaction>>`, including its empty state and subsequent updates.
5. Focused domain unit tests use a fake repository to prove all four use cases' delegation, returned IDs and Boolean results, and observed lists. Add and update tests cover `amountVnd` values of `0`, `-1`, `1`, and `Long.MAX_VALUE`, plus empty and whitespace-only categories; invalid inputs cause no repository write. Domain imports contain no Android, Compose, Room, Hilt, `data`, or `presentation` types; `presentation` does not access Room directly.
6. `TransactionEntity` stores the record ID, type, `amountVnd` as a `Long` count of whole VND, category, date, and optional note without dropping information. The mapper converts between `Transaction` and `TransactionEntity` in both directions; tests round-trip both types, `Long.MAX_VALUE` without precision loss, a date, and null and non-null notes.
7. `TransactionDao` returns a Room-backed `Flow<List<TransactionEntity>>` ordered by date descending then ID descending, plus insert, update, and delete operations. `ExpenseManagerDatabase` includes the entity and provides the DAO. Room tests cover date ordering and same-date ID ties. One test keeps a collector active and verifies that it receives the empty list and the resulting lists after insert, update, and delete, without polling or resubscribing.
8. `RoomTransactionRepository` implements `TransactionRepository`, uses `TransactionDao` for reads and writes, and maps each DAO `Flow` emission to domain `Transaction` records. It returns the inserted ID and converts DAO update/delete row counts to the contract's Boolean results. No Room type appears in the domain interface or its results. Repository tests exercise observation and writes through the interface, including the unknown-ID result.
9. Hilt provides one application-scoped `ExpenseManagerDatabase`, its `TransactionDao`, a `TransactionRepository` backed by `RoomTransactionRepository`, and all four use cases. A focused Hilt integration test resolves the add and observe use cases and sees an added record through the observed list. The `domain` package has no Hilt imports or annotations.
10. A persistence test closes and reopens a file-backed `ExpenseManagerDatabase`, then confirms the saved record and its fields remain available. An in-memory database alone does not satisfy this check. The initial schema is documented or exported for later migration review.
11. `settings.gradle.kts` still includes only `:app`. Source review confirms `presentation -> domain <- data`, with `di` wiring implementations at the app boundary: `domain` imports no Android, Compose, Room, Hilt, `data`, or `presentation` types; `data` does not depend on `presentation`; and `presentation` does not import DAO, database, or entity types.
12. The change adds no Compose UI, Home, charts, or Settings screen or behavior; the existing starter screen remains the app entry point.
13. `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug` pass with the Gradle wrapper. Run `:app:connectedDebugAndroidTest` on a device or emulator for the Room tests and record the command, device API level, and result. Do not mark the task complete while a required check is unverified.

## Test plan

| Test level | Required evidence | Gradle check |
| --- | --- | --- |
| Local unit | Use cases with a fake repository: valid and invalid amounts/categories, create ID, update/delete results, and observed empty and changed lists. Pure mapper round trips for both types, dates, notes, and `Long.MAX_VALUE`. | `:app:testDebugUnitTest` |
| Android Room | DAO and repository with a real Room database: empty result, insert/update/delete, ordering with equal dates, active `Flow` updates, unknown IDs, and data after closing and reopening a file-backed database. | `:app:connectedDebugAndroidTest` |
| Android Hilt | Resolve add and observe use cases from the app graph and verify a stored record appears in the observed list. | `:app:connectedDebugAndroidTest` |

Use test database names isolated from user data, and close test databases after each test.

## Out of scope

No Compose UI, Home, charts, or Settings work belongs to TASK-002. Record screens are planned for TASK-003, and monthly calculations for TASK-004. Account or network sync, currency conversion, and database migrations are also outside this task. Document the initial Room schema so a later schema change can be reviewed.

## Implementation order

1. Define `Transaction`, `TransactionType`, validation rules, `TransactionRepository`, and the four use cases; add focused unit tests.
2. Add `TransactionEntity`, `TransactionDao`, `ExpenseManagerDatabase`, and both mapping directions; then implement `RoomTransactionRepository`.
3. Provide the database, DAO, repository, and use cases from `di`. Test Hilt resolution, empty observation, mutations, mapping, and persistence across database reopen; run the required Gradle and device checks and record their results here.

## Verification record (2026-10-19)

- Ran `.\gradlew.bat --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`: **passed**. Local unit tests: 7 passed, 0 failed (including the existing starter test). Lint: 0 errors, 12 warnings.
- Ran `.\gradlew.bat --no-daemon :app:connectedDebugAndroidTest` on the `Pixel_5` emulator, API 35: **passed**. Android tests: 4 passed, 0 failed (including the existing starter test). The Room tests covered active `Flow` updates, CRUD, ordering, unknown IDs, and persistence after reopening a file-backed database; the Hilt test resolved use cases through an isolated in-memory database.
- The initial Room schema was exported to `app/schemas/com.ngyennhi.expensemanager.data.local.ExpenseManagerDatabase/1.json`.
- The API 26 startup check belongs to TASK-001 and is still unverified because the installed SDK has no API 26 system image.
