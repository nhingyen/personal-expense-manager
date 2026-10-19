# Copilot instructions

This is a single-module Android Kotlin portfolio app. Read `docs/PROJECT.md`, `docs/ARCHITECTURE.md`, and `tasks/ROADMAP.md` before proposing code.

- Keep Kotlin and Compose code simple and idiomatic for an Android Intern/Fresher portfolio.
- Respect `presentation -> domain <- data`; use `di` only for wiring. Domain stays pure Kotlin; Compose never accesses Room directly.
- Use Material 3, ViewModels with immutable `StateFlow` UI state, coroutines, and Flow. Room will be the offline source of truth when introduced.
- Minimum SDK is 26. Add libraries only in the task that needs them and through `gradle/libs.versions.toml`.
- Make changes small, add meaningful checks for behavior, and state which checks actually ran. Do not commit or push without an explicit request.
