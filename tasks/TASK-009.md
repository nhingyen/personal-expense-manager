# TASK-009 — Wallet Management

## Scope

Manual offline cash, bank and e-wallet management: wallet list, create/edit, details and all transactions belonging to a wallet. Enter from Settings. Reuse the existing Design System, navigation, transaction form/rows and VI/EN resources. Figma MCP references: Wallets `14:189` and Add transaction `3:9803`. Unillustrated editor/detail states extend the same Material 3 components. No transfers, bank APIs, currency conversion or unrelated settings.

Room v2 adds wallets and a required indexed foreign key on transactions. Migration 1→2 preserves every existing field/ID and assigns old records to permanent default wallet ID 1, with zero opening balance. Fresh installs create the same default. Its empty stored name has a localized default label; users may rename/edit it. The default cannot be deleted or replaced in this task. Other wallets may be deleted only when empty, after confirmation; wallets with transactions explain why deletion is blocked. Transactions must be reassigned or deleted individually first.

## Acceptance criteria

1. List shows exact total and per-wallet balances; create/edit support all three types, trimmed names (1–60 characters) and signed whole-VND opening balances within Long range. Blank/invalid input blocks writes. Negative balances are allowed. Cancel/Back never writes; pending writes block repeated actions.
2. Each balance is opening balance + all-time income − all-time expenses for that wallet. Exact arithmetic detects overflow; errors hide financial totals and offer recovery. Opening balances never become transactions or change Dashboard, Statistics or Budget totals.
3. Add defaults to wallet 1 (or the wallet invoking Add from detail). Add/edit can choose a saved wallet. Detail displays the saved wallet; moving/editing/deleting a transaction updates affected balances without duplicating records. Wallet detail lists only that wallet's transactions across all dates, with Add and record navigation.
4. Loading, observed-empty, missing-wallet, no-transactions, read/write error and validation states are localized. Retry replaces observation; failures never claim success. Existing transaction, budget, statistics and tab flows continue to work.
5. Room migration validates against the exported v2 schema, preserves v1 IDs/amounts/types/dates/categories/notes and future autoincrement behavior, and enforces foreign keys. No destructive migration. Wallet deletion is guarded atomically as well as in UI.
6. Pure domain use cases, repositories, Hilt, MVVM and immutable StateFlow follow existing dependency boundaries. Domain remains free of Android types.
7. Focused domain/ViewModel, persistence/migration and Compose tests prove the behavior. Run Build, Unit Test and Lint, record actual connected results, capture new screens from the running app if a device is available, and update README/architecture/design/roadmap. No commit/push.

## Testing

- Unit: wallet validation, signed/overflow amounts, balance isolation and changes, opening balance exclusion from existing summaries/statistics/budgets; ViewModel observation/retry/write failures and duplicate-write locking; transaction wallet preservation.
- Device: MigrationTestHelper 1→2 validation from exported schemas (populated and empty), foreign-key/delete guards, fresh install and persistence; wallet navigation/create/edit/pick/detail flows.
- Commands: `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` and focused `:app:connectedDebugAndroidTest` on the available emulator.
- Capture fictional isolated data only; verify local screenshot links, VI/EN keys and `git diff --check`.

## Results

Completed on 2026-10-26.

- Implemented manual Cash/Bank/E-wallet list, create/edit and all-time detail screens from Settings. Added exact balances, signed opening balances, wallet selection on Add/edit, saved wallet labels in transaction details and wallet-scoped record navigation. Reused shared theme, top bar, transaction rows, chips, locale formatting and Navigation; downloaded/converted four original Figma vectors locally.
- Added pure wallet models/repository/use cases, Room wallet storage, Hilt wiring and WalletViewModel StateFlow. Fresh installs and v1 migrations create wallet 1; old records retain every field and ID. The default stays permanent, and a guarded SQL DELETE plus foreign key prevents removing wallets that have transactions. Opening balances never create transaction rows or change monthly reports.
- Added 12 unit cases (5 wallet domain, 6 wallet ViewModel, 1 transaction wallet-selection regression), extended mapper checks, and added 7 connected cases (3 migration/persistence, 2 wallet flows/captures, 2 wallet screen-state/localization tests). Existing transaction UI tests now scroll to actions in the longer form; the Settings navigation assertion checks the working wallet entry.

### Verification

| Check | Actual result |
| --- | --- |
| Build | `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` completed successfully. Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. |
| Unit Test | 90 tests, 0 failures/errors/skips. Executed in the preceding combined run; up to date in the final command because application/unit-test sources were unchanged. |
| Lint | Passed on final sources: 0 errors, 16 warnings, 1 hint. Includes the previous 15 warnings/1 hint plus an available-version advisory for the debug serialization BOM. No lint baseline or disabled check was added. |
| Device tests | Full `:app:connectedDebugAndroidTest` run on Pixel_5 / Android 15 / API 35: 48 tests, 0 failures/errors/skips. Includes all existing Home, Budget, Statistics, navigation, transaction and persistence tests plus the 7 new cases. |
| Migration | MigrationTestHelper validated exported v1→v2 schemas with populated and empty records; field/ID/order/sequence preservation, unchanged report totals, default wallet, invalid FK rejection, reopen, reassignment and guarded deletion passed. |
| Wallet UI | Create/edit/select/reassign/delete, unchanged Home expense total, default/used-wallet delete protection, read-error retry/empty/overflow rendering, VI/EN editor validation and 1.5× text passed. |
| Screenshots | Four full-device 1080×2340 PNGs were captured, opened for visual review and added to README. Fixtures use a separate in-memory test database; no production seed records or image retouching. |
| Repository checks | Matching VI/EN keys/placeholders, pure domain imports, four nonempty vector assets, README image links/PNG decoding and `git diff --check` passed. No commit/push. |

Initial migration-helper execution exposed an incompatible serialization core (Navigation 1.7.3 versus Room test JSON 1.8.1); the debug-only BOM resolves that test configuration. An initial full UI run encountered a sleeping/stuck emulator keyguard with Activities remaining STOPPED and was interrupted. After reboot/unlock, all 48 device tests passed. The combined successful device run also reported a test-only Lint annotation error; the redundant `@RequiresApi` on the screenshot test was removed in favor of its existing `@SdkSuppress`, and the final Build/Unit Test/Lint command passed. Application behavior was unchanged by that annotation correction.

Only API 35 device execution is claimed; API 26 device verification remains open. Unsaved wallet drafts survive Activity recreation through the ViewModel but are not restored after process death. The default wallet cannot be replaced/deleted; other settings, transfers and bank APIs remain outside scope.

### Screenshot fixtures

`WalletFlowTest.captureWalletScreensWithIsolatedFictionalData` uses cash opening 2,000,000 VND, bank opening 5,000,000 VND and e-wallet opening 500,000 VND; bank income is 8,000,000 VND and cash expense is 85,000 VND. Total tracked balance is 15,415,000 VND. Screens are captured through UiAutomation and copied unchanged into `docs/screenshots/wallets.png`, `wallet-details.png`, `edit-wallet.png` and `transaction-wallet.png`. The picker image shows the invoking bank wallet selected. The test's public download copies are removed after review; unrelated device files are retained.
