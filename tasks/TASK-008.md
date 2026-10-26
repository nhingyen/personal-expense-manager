# TASK-008 — Portfolio README and screenshots

## Scope

Prepare the repository for CV reviewers with a clear English README and screenshots from the running Android app. This task changes documentation and image assets only; it does not complete the broader portfolio release pass or change application behavior.

## Acceptance criteria

- README explains the app's purpose and implemented transaction, Home, budget, filter, category, and Statistics features in simple English.
- Eight actual app screenshots appear in two rows of four, with descriptive labels and alternative text. Assets use repository-relative paths.
- Screenshots use English and fictional sample data, identify the capture device/API, and do not suggest that fresh installs include those records.
- README covers the current tech stack, dependency direction, local storage, setup requirements, and build/test commands. Verification limits are recorded in this task.
- Local documentation/image links resolve, PNG dimensions are consistent, and `git diff --check` passes.
- Run and record relevant Gradle checks without presenting historical device tests as a new run.

## Results

Verification date: 2026-10-26.

- Replaced the outdated README with the implemented feature overview, screenshot gallery, tech stack, package structure, data flow, setup, and testing.
- Captured Home, Transactions, Add transaction, Transaction details, Statistics, Transaction filters, Monthly budget, and Categories using ADB `screencap` on Pixel_5 / Android 15 / API 35 at 1080 × 2340 px, English (en-US), normal font scale. Images are original screen captures, without generated UI or image retouching.
- Used a temporary secondary Android user to keep sample records separate from the emulator owner's app data. The sample month has 17,500,000 VND income, 3,420,000 VND expenses, and a 5,000,000 VND spending limit. Earlier sample months populate the six-month chart. The Add form is an unsaved example.
- Stored screenshots under `docs/screenshots/` and arranged the gallery in two four-column Markdown tables. No production seed data, dependency, module, schema, or application source change.

| Check | Result |
| --- | --- |
| `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` | Passed; build and existing test results were up to date. |
| `.\gradlew.bat :app:testDebugUnitTest --rerun :app:lintDebug --rerun` | Passed. Unit test task reran: 69 tests, 0 failures/errors/skips. Lint lifecycle task reran; analysis remained up to date. Report: 0 errors, 15 warnings, 1 hint. |
| Instrumented tests | Not rerun for this documentation-only task. Prior API 35 feature verification is recorded in TASK-007; it is not a new result here. |

Final checks passed: all eight PNG files have valid chunk checksums, decode successfully, and measure 1080 × 2340 px. The gallery has two rows with four images each, consistent display widths, and alternative text. README/task/roadmap local links resolve, and `git diff --check` passes. The screenshots total 1,157,310 bytes (about 1.1 MiB).

Visually reviewed all eight final captures. No keyboard or system onboarding dialog covers the app; the Statistics capture is scrolled to show both charts and all six month labels. Returned the emulator to Owner (user 0), removed the temporary screenshot user and its sample data, and removed temporary device capture files. The owner's app data was not edited.

Remaining verification limits: API 26 startup, a full spoken TalkBack traversal, and a new full offline release smoke test were not performed in this documentation task. Settings remains a placeholder, as documented in the project brief and feature roadmap.
