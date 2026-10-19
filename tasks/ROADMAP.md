# Roadmap

Work in task order. Each task should produce one reviewable PR; update scope and acceptance criteria before implementation when requirements change. [TASK-000.md](TASK-000.md) expands the foundation task.

| Task | Outcome | Acceptance criteria |
| --- | --- | --- |
| TASK-000 | Development foundation | Project and architecture docs agree; branch/PR/review process is documented; installable tracked Git hooks check staged whitespace; GitHub Actions runs lint, unit tests, and debug build for PRs and the default branch; a PR checklist records review and verification. |
| TASK-001 | Architecture and dependency setup | One `:app` module remains; Room, Hilt, and coroutine/Flow dependencies are added through the version catalog; package boundaries follow `ARCHITECTURE.md`; the app builds and unit tests run. |
| TASK-002 | Expense domain and local storage | Domain model and repository contract cover income/expense records; Room stores amount in integer minor units and supports observe/create/update/delete; mapping and persistence tests pass, including an empty database and a reopened database. |
| TASK-003 | Record management UI | Compose list and add/edit screens use Material 3 and ViewModels; users can create, edit, and delete records offline; validation prevents invalid amounts; UI state covers empty/loading/error; core flow is verified by focused tests or documented manual checks. |
| TASK-004 | Monthly summary | Selected month shows income, expense, and balance derived from stored records; updates appear after record changes; calculation and month-boundary tests pass. |
| TASK-005 | Portfolio release pass | README has setup, screenshots, and architecture summary; accessibility labels and screen states are reviewed; lint, unit tests, debug build, and a manual offline smoke test pass; known limitations are documented. |

TASK-000 is documentation and development workflow only. App features and new dependencies begin in later tasks.
