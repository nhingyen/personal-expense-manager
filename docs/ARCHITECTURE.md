# Architecture

## Single-module layout

Keep one Gradle module, `:app`, with packages under `com.ngyennhi.expensemanager`:

| Package | Responsibility |
| --- | --- |
| `domain` | Pure Kotlin models, repository interfaces, and business rules/use cases. |
| `data` | Room entities/DAO/database, mappers, and repository implementations. |
| `presentation` | Compose screens, ViewModels, UI state, and event handling. |
| `di` | Hilt bindings and application wiring. |

The existing starter `ui.theme` package can move under `presentation` when the first feature is built; no move is needed for documentation work.

## Dependency rules

`presentation -> domain <- data`; `di` connects implementations to domain interfaces at the app boundary. Domain must not import Android, Compose, Room, Hilt, or presentation/data classes. Data must not depend on presentation. Compose screens must not call DAOs or know Room entities. These package rules are enforced by review while the code remains in one module.

## Data and state flow

Room is the future local source of truth. A repository exposes domain models and `Flow` streams; use cases express business rules. ViewModels collect flows with lifecycle-aware coroutines, expose immutable `StateFlow<UiState>`, and handle user actions. Compose renders state and sends events to ViewModels. Use suspend functions for writes and explicit error handling for failures. Keep currency values as integer minor units and dates as a stable domain representation; mapping to storage and display formats stays at the boundaries.

## Testing

Test domain rules with local unit tests; test repository mapping and observable updates when Room is introduced; add focused UI tests for important user flows. Each roadmap task names the checks that prove its acceptance criteria.
