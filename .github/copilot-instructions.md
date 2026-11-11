# Copilot instructions

`AGENTS.md` is the canonical source of shared repository guidance. This is a single-module Android Kotlin portfolio app; read `docs/PROJECT.md`, `docs/ARCHITECTURE.md`, and `tasks/ROADMAP.md` before proposing code.

- Keep Kotlin and Compose code simple and idiomatic for an Android Intern/Fresher portfolio.
- Respect `presentation -> domain <- data`; use `di` only for wiring. Domain stays pure Kotlin; Compose never accesses Room directly. Room is the offline source of truth.
- Use Material 3, ViewModels with immutable `StateFlow` UI state, coroutines, and Flow.
- Minimum SDK is 26. Add libraries only when the work needs them and through `gradle/libs.versions.toml`.
- For small work, discover and implement directly. For normal work, give a concise conversational plan and request approval for significant decisions. For complex or high-risk work, use a persistent plan and obtain explicit approval before implementation.
- Do not create `TASK-###.md` for ordinary features or fixes. Keep changes small, add meaningful checks, and never claim verification passed unless the command ran.
- After implementation, explain changed files and data flow, architectural decisions, and relevant Kotlin/Android concepts; ask two or three questions to check understanding.
- Do not commit or push without an explicit request.
