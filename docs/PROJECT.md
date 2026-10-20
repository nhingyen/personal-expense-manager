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

The repository has a localized starter screen in Vietnamese (default) and English. Compose and Material 3 remain configured. The single app module now has a pure domain transaction contract and use cases, Room persistence, a repository, and Hilt wiring; local and emulator tests pass on API 35. Minimum-SDK startup verification on API 26 remains open. Transaction screens and other application behavior remain planned. The [roadmap](../tasks/ROADMAP.md) defines the order of work.
