# Repository guidance

Personal Expense Manager is an Android Kotlin portfolio project. Read [docs/PROJECT.md](docs/PROJECT.md), [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), and [tasks/ROADMAP.md](tasks/ROADMAP.md) before changing app code.

- Keep one `:app` module until a measured need for another module exists. Minimum SDK is 26; use Kotlin, Jetpack Compose, and Material 3.
- Follow the dependency rules in `docs/ARCHITECTURE.md`: presentation and data depend on domain; domain depends on neither. Keep Android, Compose, Room, and Hilt types out of domain.
- Before implementing UI, read docs/DESIGN.md and consult the linked Figma design via MCP.
- Prefer small, reviewable changes tied to a roadmap task, issue, or clearly stated request. Add dependencies only when the work needs them, through the version catalog.
- Give roadmap-scale work observable acceptance criteria and update its documentation when scope changes. Do not create `TASK-###.md` for ordinary features or fixes. Add focused tests for domain rules and meaningful data or UI behavior.
- After every feature task, update README.md to match the scope actually implemented and verified. Do not describe unimplemented features or claim checks that did not run.
- Before proposing a PR, run the relevant Gradle checks and report what ran. Never claim verification passed unless the command actually ran.
- Do not commit, push, or alter Git history unless the user explicitly requests it.

## AI-assisted development workflow

Use `DISCOVER → PLAN → IMPLEMENT → VERIFY → REVIEW → LEARN → COMMIT`, proportionate to the work:

- **Small:** perform focused discovery, then implement directly.
- **Normal:** give a concise conversational plan; request approval when significant scope, behavior, or architectural decisions are involved.
- **Complex or high-risk:** create a persistent plan and obtain explicit approval before implementation. This includes migrations, durable-data changes, security-sensitive work, and broad cross-layer changes.

After implementation, verify the relevant behavior and inspect the complete diff. Learning Mode is mandatory: explain the changed files and data flow, the architectural decisions and relevant Kotlin/Android concepts, then ask two or three questions to check understanding. The commit step is only available after explicit authorization; the same applies to pushing.
