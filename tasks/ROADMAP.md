# Roadmap

Work in task order. Each task should produce one reviewable PR; update scope and acceptance criteria before implementation when requirements change. [TASK-000.md](TASK-000.md) expands the foundation task, and [TASK-002.md](TASK-002.md) defines the next storage task's observable checks.

| Task | Outcome | Acceptance criteria |
| --- | --- | --- |
| TASK-000 | Development foundation | Project and architecture docs agree; branch/PR/review process is documented; installable tracked Git hooks check staged whitespace; GitHub Actions runs lint, unit tests, and debug build for PRs and the default branch; a PR checklist records review and verification. |
| TASK-001 | Architecture, dependencies, and localization | One `:app` module remains with MVVM/Clean Architecture boundaries; Room, Hilt, KSP, and coroutine/Flow setup uses the version catalog; Vietnamese is the default with English resources and Android 13+ system per-app language support; build, unit tests, lint, and startup checks pass. |
| TASK-002 | Transaction Domain & Room Persistence | Domain model and repository contract cover income/expense records; Room stores positive `Long` amounts in whole VND and supports observe/create/update/delete; mapping and persistence tests pass, including an empty database and a reopened database. |
| TASK-003 | Transaction UI & MVVM Integration | Compose list and add/edit screens use Material 3 and ViewModels; users can create, edit, and delete records offline; validation prevents invalid amounts; UI state covers empty/loading/error; core flow is verified by focused tests or documented manual checks. |
| TASK-004 | Monthly Home + MVVM integration | Home follows the Figma greeting, bell, summary, Budget/Categories shortcuts, monthly-budget progress, and recent-record layout; spending limits persist locally and progress follows Room expenses; categories prefill Add and the bell opens actual budget notices. Bottom navigation is tracked separately in TASK-005. Month survives navigation/recreation; View all, floating/empty-state Add, Android Back, and direct detail/save/delete paths work; calculation, state, persistence, localization, and navigation checks are recorded in [TASK-004.md](TASK-004.md). |
| [TASK-005](TASK-005.md) | Bottom Navigation & App Navigation | Figma Home/Transactions/Add/Statistics/Settings bar reuses existing screens and Design System; Statistics/Settings are localized placeholders; destination selection, Back, save/restore state and hidden bar on secondary screens are tested; build, unit tests and lint are recorded. |
| [TASK-006](TASK-006.md) | Transaction Search & Filter | Search note/category; combine type, category and inclusive date range; reset, no-match actions and VI/EN reuse existing MVVM/Room/UI; focused search/filter tests, build, unit tests and lint are recorded. |
| TASK-007 | Portfolio release pass | README has setup, screenshots, and architecture summary; accessibility labels and screen states are reviewed; lint, unit tests, debug build, and a manual offline smoke test pass; known limitations are documented. |

TASK-000 is documentation and development workflow only. App features and new dependencies begin in later tasks.
An in-app language picker is deferred to a future Settings task.
