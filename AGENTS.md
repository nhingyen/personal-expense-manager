# Repository guidance

Personal Expense Manager is an Android Kotlin portfolio project. Read [docs/PROJECT.md](docs/PROJECT.md), [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), and [tasks/ROADMAP.md](tasks/ROADMAP.md) before changing app code.

- Keep one `:app` module until a measured need for another module exists. Minimum SDK is 26; use Kotlin, Jetpack Compose, and Material 3.
- Follow the dependency rules in `docs/ARCHITECTURE.md`: presentation and data depend on domain; domain depends on neither. Keep Android, Compose, Room, and Hilt types out of domain.
- Prefer small, reviewable changes tied to a roadmap task. Add dependencies only when the task needs them, through the version catalog.
- Give each task observable acceptance criteria and update its documentation when scope changes. Add focused tests for domain rules and meaningful data or UI behavior.
- Before proposing a PR, run the relevant Gradle checks and report what ran. Do not claim a check passed unless it ran.
- Do not commit, push, or alter Git history unless the user explicitly requests it.
