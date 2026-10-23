# Project brief

## Goal

Build an offline personal expense tracker that demonstrates clear Android fundamentals at Intern/Fresher level: readable Kotlin, reactive UI, persistence, tests, and a reviewable development process.

## MVP

- Record income and expenses with amount, category, date, and optional note.
- View, edit, and delete records; show a monthly income, expense, and balance summary.
- Persist records locally with Room so core flows work without an account or network.
- Handle empty, loading, and error states in a usable Material 3 Compose UI.

Money will be stored as a positive `Long` count of whole VND; display formatting belongs to presentation. The initial release has no sync, authentication, OS notification delivery, or currency conversion. TASK-004 additionally supports a local monthly spending limit and in-app budget notices.

## Technical direction

Kotlin, Jetpack Compose, Material 3, MVVM plus Clean Architecture, a single `:app` module, coroutines and Flow/StateFlow, Hilt for injection, and Room for local storage. Minimum SDK: 26. See [ARCHITECTURE.md](ARCHITECTURE.md) for dependency rules.

## Current state

The single app module has pure domain transaction, monthly-summary, and monthly-budget rules, Room transaction persistence, Hilt wiring, and Compose Dashboard/list/add/edit/detail flows with ViewModel StateFlow and Navigation. Home shows a clock-based greeting, selected-month totals, Budget/Categories shortcuts, an editable persisted monthly spending limit with actual expense progress, budget notices, and up to five recent Room records. Categories reuse form suggestions and saved transaction categories to prefill Add. Vietnamese is the default locale and English resources are provided. TASK-004 verification is recorded in its task document; minimum-SDK startup verification on API 26 from TASK-001 remains open. The [roadmap](../tasks/ROADMAP.md) defines the order of work.
