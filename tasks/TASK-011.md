# TASK-011: Firebase Authentication & Guest Mode

> Historical record: this authentication implementation and its setup instructions are retired from v1.0.0 by [TASK-013](TASK-013.md). The current app is offline-only and has no login/account UI. Verification below applies to the earlier implementation.

## Scope

Add optional Firebase Email/Password login and registration, persisted Firebase auth state, logout and offline Guest Mode. Home remains the startup route; Settings exposes the current account or Guest and opens a secondary authentication screen. Reuse MVVM, Hilt, immutable StateFlow, the Design System and VI/EN resources in the single `:app` module.

Room wallets/transactions and local budgets/settings remain device-wide. Authentication never merges, deletes, migrates, filters or synchronizes them by account. Both Settings and authentication explain that financial data is local and is not protected separately for each account. Guest Mode is local usage, not Firebase anonymous authentication.

Google Sign-In extends this task through [TASK-012](TASK-012.md), using the same auth state and navigation. Firestore, Cloud Sync, password reset, account deletion and other new features remain outside scope.

## Acceptance criteria

- [x] Confirm resolved Firebase/Kotlin/AGP compatibility before implementation; keep dependencies in the version catalog.
- [x] Guest can launch and use all existing offline features without authentication or network.
- [x] Settings displays restored account email or Guest; Login/Register are secondary routes without the bottom bar; Back/Continue as guest return to Settings.
- [x] Email/password login and registration validate email, required password, registration minimum six characters and matching confirmation. Preserve password whitespace and trim email only.
- [x] Loading blocks duplicate submissions, mode changes and leaving an in-flight operation. Errors are localized for credentials, email already in use, weak password, network, throttling, disabled user/provider and unknown failures, with retry through the form.
- [x] Firebase owns persisted session state; a cancellable auth listener observes login/logout/restoration. Logout works locally and returns to Guest (SDK implementation and fake-backed tests; live backend verification remains open below).
- [x] Passwords are masked, held only in ordinary composition memory and request scope, never logged or stored in SavedStateHandle/preferences/Room/StateFlow. Leaving/recreating the form clears passwords.
- [x] Room schema, records, wallets and preferences are unchanged across Guest → account A → Guest → account B; UI explains the device-wide data boundary.
- [x] Every new user-facing string has matching VI/EN resources; dark appearance, scrolling, IME/system insets and accessible errors reuse current components.
- [x] CI builds with a clearly non-production Firebase config when no real config is supplied; local Firebase config is ignored; README documents setup and actual verification (verified locally with CI config, not a remote Actions run).

## Testing

Run dependency insight, debug build, unit tests and lint. Add focused tests for pure validation, repository listener cleanup/error mapping/cancellation, auth ViewModel restoration/loading/retry/logout, localized Compose states and Room preservation across session transitions. Compile device tests even if no emulator is available. Real Firebase success/error/session-restoration checks need Email/Password enabled in a configured Firebase project; do not claim them from fake-backed tests.

### Auth visual refinement

At the user's request, refine Login/Register using the existing Home/Add Figma language: compact pastel welcome card, clearly selected Login/Register pills, consistent field typography, primary/Guest action hierarchy and a concise visible device-data disclosure. Keep Firebase, validation, loading, password lifetime and financial storage behavior. Recapture actual Login/Register screens and place them first in README.

- [x] Read Home `3:9610` and Add `3:9803` via Figma MCP; reuse shared semantic colors, typography, existing wallet/back assets, 20 dp margins and rounded shapes, without gradients or new dependencies.
- [x] Verify selected/disabled mode controls, VI/EN errors, dark/large text, password masking/clearing and existing auth/Room flow tests.
- [x] Run Build/Unit Test/Lint, inspect fresh app screenshots and update DESIGN/README with auth images before other screenshots.

## Verification

- Before implementation: `:app:dependencyInsight --dependency firebase-auth --configuration debugCompileClasspath` passed; BoM 34.10.0 selects Auth 24.0.1 with AGP 9.4.1. Existing Kotlin/Compose compiler 2.2.10, KSP 2.3.12 and Hilt 2.60.1 are retained. This also retains the previously verified TASK-001 Kotlin metadata compatibility fix.
- Figma Settings `3:10256` was consulted through MCP design context and screenshot. Auth forms have no reference frame and compose the existing theme/top bar/Material fields and buttons. Existing Settings vectors/top-bar/navigation assets are reused without new downloads.
- `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` passed. All **112 unit tests** passed, including **16 new auth tests** (3 validation, 5 repository, 8 ViewModel). Lint: **0 errors, 16 warnings, 1 hint**, including the new coroutine dependency update suggestion; SDK/compiler versions were not upgraded to silence warnings.
- Pixel_5/API 35: **9 device tests passed**, with `:app:connectedDebugAndroidTest` run separately for each class: `com.ngyennhi.expensemanager.presentation.AuthScreenTest` (5), `AuthFlowTest` (2) and existing `SettingsScreenTest` (2). Tests verify VI/EN labels/errors, dark/200% text, masked/ephemeral passwords, disabled loading controls, account email/logout confirmation, navigation/recreation, network failure/Guest/retry and exact preservation of Room transactions, wallets/balances and budget limits across two accounts and Guest. AuthFlow uses fake auth and an isolated in-memory Room database/preferences; it does not contact Firebase.
- The first AuthFlow run failed in Espresso's keyboard helper because the emulator window lacked focus. The helper now hides IME through WindowInsetsController; both flows subsequently passed. A comma-separated runner class argument ran only the first class on this setup, so final class results were checked individually in XML reports.
- All **30 new auth resource keys** match between VI/EN; `git diff --check` passed.
- Executed the workflow's Python config step in isolated directories: missing secret and valid JSON succeed, invalid JSON and wrong package IDs fail without exposing supplied content. An independent copy under ignored `build/task011-ci` uses only `app/google-services.example.json`, not the real local Firebase config; `--no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` passed there, including 112 unit tests. The actual local Firebase file was never overwritten and `git check-ignore` confirms it is ignored.
- README, ROADMAP, PROJECT, ARCHITECTURE and DESIGN reflect implemented scope. No commit, push, account creation against a live Firebase project or Git history operation was performed.

### Auth visual refinement verification (2026-10-26)

- `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest` passed after the UI/resource changes. **112 unit tests** passed; lint has **0 errors, 16 warnings, 1 hint**. No dependency, auth repository, ViewModel or Room changes were needed.
- Separate `:app:connectedDebugAndroidTest` class runs passed **10 API 35 device tests**: `AuthScreenTest` (6, including the new mode-tab selection test), `AuthFlowTest` (2) and `SettingsScreenTest` (2). VI/EN errors, dark/200% text, selected/disabled tabs, password masking/clearing and unchanged financial data remain covered. XML results were retained locally under ignored `build/task011-redesign-reports`.
- All **31 auth resource keys** match in VI/EN. The removed instructional copy is documented in the architecture/task instead of occupying the form; password handling remains unchanged. System Back now uses the same dismissal callback as the visible Back/Guest controls.
- Fresh **1080 × 2340** Login/Register PNGs were captured from the actual app on Pixel_5/API 35 in English/light appearance and visually inspected. Welcome card, wallet/back assets, selected tabs, fields, primary/Guest actions and local-data note are visible without scrolling at default text size. No image editing or generated mockup was used.
- `docs/screenshots/auth-login.png` and `auth-register.png` are the first two images in README, followed by the existing financial gallery. README alt text/results and DESIGN were updated; local links, image existence/order and `git diff --check` passed. No commit/push was performed.

### Runtime limits / follow-up verification

- [ ] With a real configured Firebase project and Email/Password enabled, manually verify registration/login errors and success, cold-start/process-death session restoration, offline restored-session display and logout. No live Firebase backend behavior is claimed from fake tests.
- [ ] API 26 runtime check remains open from TASK-001/TASK-010; this task's device checks used API 35.
- GitHub Actions itself was not triggered because no push/PR was requested. Its build/config path was verified locally as described above.

References: [Firebase Android releases](https://firebase.google.com/support/release-notes/android), [Email/Password setup](https://firebase.google.com/docs/auth/android/password-auth), [AGP 9.4 compatibility](https://developer.android.com/build/releases/agp-9-4-0-release-notes).

### TASK-012 extension

Google Sign-In acceptance criteria, Firebase Console prerequisites, tests and final verification are tracked in [TASK-012](TASK-012.md). The earlier verification records above apply to the Email/Password implementation before this extension.
