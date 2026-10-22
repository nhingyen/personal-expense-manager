# Project brief

## Goal

Build an offline personal expense tracker that demonstrates clear Android fundamentals at Intern/Fresher level: readable Kotlin, reactive UI, persistence, tests, and a reviewable development process.

## MVP

- Record income and expenses with amount, category, date, and optional note.
- View, edit, and delete records; show a monthly income, expense, and balance summary.
- Persist records locally with Room so core flows work without an account or network.
- Handle empty, loading, and error states in a usable Material 3 Compose UI.

Money will be stored as a positive `Long` count of whole VND; display formatting belongs to presentation. The initial release has no sync, authentication, budget alerts, or currency conversion.

## Technical direction

Kotlin, Jetpack Compose, Material 3, MVVM plus Clean Architecture, a single `:app` module, coroutines and Flow/StateFlow, Hilt for injection, and Room for local storage. Minimum SDK: 26. See [ARCHITECTURE.md](ARCHITECTURE.md) for dependency rules.

## Current state

The single app module has a pure domain transaction contract and use cases, Room persistence, Hilt wiring, and a Compose transaction list/add/edit/detail flow with ViewModel state and Navigation. Vietnamese is the default locale and English resources are provided. TASK-003 build, unit, lint, and emulator tests have run on API 35; minimum-SDK startup verification on API 26 from TASK-001 remains open. Monthly summary and other application behavior remain planned. The [roadmap](../tasks/ROADMAP.md) defines the order of work.
