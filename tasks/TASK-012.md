# TASK-012: Google Sign-In extension to TASK-011

## Scope

Extend TASK-011's existing Email/Password authentication with an explicit Google sign-in button on Login, using Credential Manager, Google ID and Firebase Authentication. Reuse AuthRepository, AuthViewModel, the observed session and current navigation. Keep Guest Mode, Room v2 and all financial preferences independent of authentication. No Cloud Sync, account linking, reset/deletion or dependency upgrades.

This file did not exist at the start of the request; it records the requested extension rather than a separate authentication implementation.

## Acceptance criteria

- [x] Email/Password login/register, restored sessions and logout retain their behavior in the existing fake-backed regression tests.
- [x] Login offers a localized Continue with Google action, following the existing Design System and Google branding, with duplicate-submit/loading protection.
- [x] Credential Manager's explicit button flow exchanges a Google ID token through the same Firebase repository; user cancellation is silent, failures are localized and retryable in the shared auth state.
- [x] Tokens never enter StateFlow, saved state, Room, preferences or logs. Activity destruction cancels the credential request without retaining the Activity in the ViewModel.
- [x] Logout signs out Firebase and clears Credential Manager state; provider cleanup failure cannot prevent Guest access in the repository test.
- [x] No email-based account lookup, explicit credential linking, overwrite call or fallback account creation. Provider collision errors explain how to use the original sign-in method.
- [ ] Firebase must use separate accounts per provider for duplicate email addresses. Google sign-in remains blocked until the developer explicitly confirms this console configuration for the build; default Firebase automatic trusted-provider linking is not safe for this requirement.
- [x] BoM 34.10.0 / Auth 24.0.1, Kotlin 2.2.10, AGP 9.4.1, Hilt 2.60.1 and KSP 2.3.12 remain unchanged. Only Credential Manager and Google ID dependencies are added through the catalog.
- [x] Focused repository/ViewModel/UI tests cover success, cancellation, failure/retry, duplicate submission and financial data preservation; Build, Unit Test and Lint results are recorded accurately.
- [x] README, TASK-011, architecture/design/scope documentation and VI/EN resources describe the implemented extension and required Firebase Console setup.

## Verification

`tasks/TASK-012.md` did not exist before this request, so it now records the extension and these acceptance criteria. The repository's existing Firebase Email/Password, Guest, Room and Settings tests continue to run alongside the new tests.

- `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --console=plain`: **passed** in 5m 4s. Debug APK and Android test APK built; **117 local unit tests passed** across 20 suites (0 failures/errors/skips); lint: **0 errors, 21 warnings, 1 hint**. The warnings do not point to the new auth files.
- `.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.ngyennhi.expensemanager.presentation.AuthScreenTest' --console=plain`: **passed** on Pixel_5/API 35, **8 tests**. They cover VI/EN Google label, Login-only placement, callback, loading disabled state and existing auth form behavior.
- The same command with `AuthFlowTest`: **passed** on Pixel_5/API 35, **3 tests**. The added Google fake-auth session test checks Guest → Google → Guest without changing Room transactions, wallet balances or saved budget. Earlier Email/Password/Guest tests also passed.
- VI/EN `auth_*` resource keys match; `git diff --check` passed. The only new runtime libraries are AndroidX Credential Manager 1.3.0 and Google ID 1.1.1. The multicolor Google G is from Google's published identity asset.
- A first device-test command omitted PowerShell quoting around `-Pandroid.testInstrumentationRunnerArguments.class=...` and Gradle rejected it before running tests. Quoting the complete property made both class runs pass.

**External validation still open:** A real Firebase project must enable Google, supply SHA-1 and a Web OAuth client, and set **Create multiple accounts for each identity provider**. The opt-in build property defaults to false, so Google token exchange is blocked until the console setting is confirmed. Test a password account and a Google account with the same email on a disposable project before relying on the policy. The client cannot read or enforce a later console policy change. Live Google/Firebase sign-in, cross-provider behavior and API 26 runtime have not been verified; fake-backed tests do not prove them. No Cloud Sync, commit or push was performed.

## References

- [Android Credential Manager Google button flow](https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation)
- [Firebase Google authentication](https://firebase.google.com/docs/auth/android/google-signin)
- [Firebase automatic provider linking behavior](https://firebase.google.com/docs/auth/users/#verified_email_addresses)
- [Separate accounts with the same email](https://support.google.com/firebase/answer/9134820)
- [Google sign-in branding](https://developers.google.com/identity/branding-guidelines)
