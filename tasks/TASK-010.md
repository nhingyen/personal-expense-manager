# TASK-010 — Settings & Language

## Scope

Implement the Settings bottom destination using Figma Settings `3:10256`: language,
wallet entry, appearance and About/version. Reuse Material 3, navigation, VI/EN
resources, MVVM/StateFlow and the existing single-module dependency boundaries.
Support API 26+: System Default / Vietnamese / English and System / Light / Dark.
Persist theme in app-private SharedPreferences (the existing budget storage pattern);
use AndroidX AppCompat automatic locale storage below API 33 and framework per-app
locales on API 33+. No second language preference that could diverge from Android Settings.

No authentication, profile, cloud sync, backup/export, currency picker or delete-all.
No transaction schema changes or unrelated feature refactoring.

## Acceptance criteria

- Settings is reachable and selected in the existing bottom bar; Wallets still opens.
- Figma card geometry, spacing, typography and local icons are reused; controls scroll,
  support VI/EN and large text, expose selection semantics and have 48 dp touch targets.
- Three language choices update labels and locale-sensitive formatting immediately;
  System clears the app override. API 33+ external per-app language changes are reflected.
- Three theme choices update all shared surfaces/text and Material 3 components;
  System follows device appearance. Selections survive Activity recreation and relaunch.
- Locale changes use normal configuration recreation: navigation, saveable screen state,
  selected months, filters and activity ViewModel drafts survive. Theme recomposition
  does not replace the navigation host. Room transactions/wallets and budget data are untouched.
- About describes the offline app and displays the installed build's actual version.
- Read/write failures expose localized recovery UI; duplicate preference writes are blocked.
- Focused unit and instrumented tests cover options, persistence, changes and state/data safety.
- README/architecture/design/roadmap match implemented scope and actual verification.

## Testing

Run `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug` and compile/run focused
Android tests where devices are available. Cover all theme/language choices, System
fallback, preference restoration, errors/retry, navigation/recreation, transaction drafts,
filters/months and unchanged Room data. Record API levels and untested cases explicitly.

## Verification

Implemented on 2026-10-26. Settings uses the existing primary navigation route,
Wallets entry and shared Material 3 components. AppCompat 1.8.0 is the only new
runtime dependency and is declared in the version catalog. Language storage follows
the [Android per-app language guidance](https://developer.android.com/guide/topics/resources/app-languages).
The theme repository reuses the existing SharedPreferences pattern; it never opens
or changes the Room database. About reads the installed package version (`1.0` in
this build), not the Figma example version. No commit or push was made.

Figma MCP design context and screenshots were consulted for Settings `3:10256`.
Add `3:9803`, Transactions `3:9699` and Detail `3:9871` were additionally consulted
when dark review exposed opaque backgrounds in nine legacy PNG icons. Those icons
now use their original exported SVGs converted to transparent Android vectors.
Dark-only color filters preserve light appearance and improve dark icon contrast.
The transaction date/category popup flags now use rememberSaveable; no transaction
business rule, Room schema or existing navigation policy changed.

### Focused coverage

- **Unit:** all theme/system combinations and preference fallback; all language and
  theme writes, restoration, external-language refresh, no-op choices, read retry,
  failed writes and duplicate-write blocking. Six new tests; the full unit suite
  contains 96 tests with zero failures/errors/skips.
- **SettingsFlowTest (API 35):** six tests passed. Covers VI/EN/System labels and
  selections, Android Settings locale changes, Light/Dark/System including device
  night-mode changes, selected tab/Back/Wallets, exact installed version, Activity
  recreation, preserved month/query/type/category/range, an active transaction
  editor/date dialog, unchanged drafts and unchanged Room transaction snapshots.
- **SettingsScreenTest (API 35):** two tests passed. Choices/About remain reachable
  at 200% font scale in dark appearance; read failure disables writes and exposes Retry.
- **AndroidSettingsRepositoryTest (API 35):** one test passed. Defaults and every
  theme survive repository reopening, committed values are read directly from the
  on-disk preferences XML, and unknown values fall back to System.
- Screenshots in `docs/screenshots/settings-vi-light.png` and
  `settings-en-dark.png` were captured from the running app using an isolated
  in-memory Room fixture, without image edits. They show the actual installed version.

The API 35 regression runs also passed **HomeFlowTest: 16 tests** and
**WalletFlowTest: 2 tests** (navigation, scroll/recreation, CRUD, budgets, filters,
wallet selection and unchanged report totals). Together with the nine Settings tests,
**27 device tests passed**, with zero failures/skips in the recorded final runs.

Device classes were run sequentially with
`:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<fully-qualified-class>`.
Local per-class JUnit result copies are retained under `app/build/reports/task010/`.
The full connected suite was not run as one unfiltered invocation.

Final core check command completed with **BUILD SUCCESSFUL**:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

The full local unit suite ran successfully with 96 tests; unchanged unit/build outputs
were up-to-date on the final invocation. Lint reports **0 errors, 16 warnings and
1 hint** (existing dependency updates, unused colors/launcher assets, redundant
activity label and Home state-allocation hint). HTML reports remain under
`app/build/reports/`. `git diff --check` also passed.

### Platform limits

Minimum SDK remains 26 and language switching uses AppCompatActivity/automatic locale
storage for API 26–32, with framework per-app locales on API 33+. An API 26 Google APIs
x86_64 image and AVD were prepared, but Android Emulator 37.1.11 crashed before app
startup in ANGLE/D3D11 (`StateManager11::syncState` assertion), including software
renderer attempts. **API 26 runtime verification remains open; it did not pass.**
Other API levels were not exercised in this task. Device tests ran on Pixel_5/API 35.

Activity recreation and repository/disk restoration were tested; a separate manual
cold-process relaunch was not run. Existing unsaved form/filter process-death
limitations remain. Saved preferences and Room data use persistent storage. The
dark palette is an implementation extension because Figma contains only light frames.
