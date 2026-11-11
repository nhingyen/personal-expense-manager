# TASK-000: Development foundation

**Status:** In progress

**Scope:** Git workflow, project documentation, local Git hooks, GitHub Actions CI, and code review process. No app feature or dependency changes.

## Deliverables

1. Keep `README.md`, `docs/PROJECT.md`, `docs/ARCHITECTURE.md`, `tasks/ROADMAP.md`, `AGENTS.md`, and `.github/copilot-instructions.md` concise and consistent.
2. Use task branches from the default branch for roadmap work: `feature/task-<id>-<slug>` for features (for example, `feature/task-000-foundation`) and `fix/task-<id>-<slug>` for fixes. Ordinary work may use a concise feature/fix branch tied to an issue or clearly stated request; it does not require a new `TASK-###.md`. Keep commits focused; use conventional subjects such as `docs: define project plan`. Open a PR against the default branch and link its task, issue, or request.
3. Keep tracked `.githooks/pre-commit` and `.githooks/pre-push` hooks. Activate once per clone with `git config core.hooksPath .githooks`. Pre-commit checks staged whitespace and unit tests; pre-push builds and runs lint. Hooks are clone-local feedback, so CI remains the shared gate.
4. Keep a GitHub Actions workflow for PRs and pushes to `main` and `develop`. On a clean checkout, set up the required JDK and Android SDK, then run `:app:assembleDebug`, `:app:testDebugUnitTest`, and `:app:lintDebug` with the Gradle wrapper. Upload unit test reports when available. Do not require emulator tests for this foundation task.
5. Add a PR checklist covering scope, test evidence, screenshots when UI changes, reviewer notes, and key decisions or learning. For a solo portfolio PR, self-review the diff and resolve findings before merge; request another reviewer when available. Require passing CI before merge.
6. Use the proportional AI-assisted workflow in `AGENTS.md`: direct implementation for small work, a concise conversational plan for normal work, and a persistent approved plan for complex or high-risk work. Numbered task files are reserved for roadmap-scale work that needs durable scope or acceptance criteria.

## Acceptance criteria

- Documentation describes the same MVP, one-module architecture, and current versus planned dependencies without contradiction.
- Hook activation is reproducible from the documented command; staged whitespace, test, build, or lint failure blocks the corresponding Git operation without changing files.
- PRs targeting `main` or `develop` trigger CI and all three Gradle commands pass; the same checks run on pushes to those branches. Test reports are available as an artifact when generated.
- A sample PR can be reviewed using the checklist, with test results and any limitations recorded before merge.

The automation files are present. Verify a real CI run before marking TASK-000 complete; no app features or dependencies belong in this task.
