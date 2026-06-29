# Agentic Task Manager

An Android task manager that exposes its operations as [AppFunctions](https://developer.android.com/ai/appfunctions) so an on-device agent (Gemini) can call them from natural language. The focus is the agent integration and a measurement of how reliably an agent uses it; the UI is intentionally minimal.

## Overview

AppFunctions is the Jetpack layer that lets an app publish selected operations as tools an agent can invoke, roughly the Android counterpart of MCP. This project publishes five of them over a domain that has enough structure to be worth reasoning about: tasks form a dependency graph rather than a flat list.

Everything turns on one rule, actionability. A task can depend on other tasks, and it is actionable only once every task it depends on is completed. Actionability is computed by walking the graph rather than stored, so completing one task immediately re-derives what its dependents can do next. The dependency edges have to stay acyclic, and an edge that would close a cycle is rejected.

## Project layout

Two Gradle modules, with a strict boundary between them.

```
:domain   pure Kotlin/JVM, no Android
  model/        Task, TaskId, Priority, TaskStatus, Recurrence, DependencyEdge
  graph/        DependencyGraph (cycle detection), Actionability, TaskInsight
  usecase/      AddTask, AddDependency, CompleteTask, DeleteTask, queries, ObserveTaskBoard
  repository/   TaskRepository (interface)

:app      Android
  data/         Room entities, DAOs, mappers, repository implementation, seeder
  agent/        @AppFunction adapters and @AppFunctionSerializable DTOs
  ui/           Jetpack Compose
  di/           Hilt
```

`:domain` does not apply the Android Gradle plugin, so it cannot reference Android even by accident; that is enforced by the compiler, not by convention. Dependencies run inward toward it. The UI depends on use cases, the data layer implements a domain interface, and Hilt is the only place that binds an implementation to an interface. The use cases carry no DI annotations and are constructed in a Hilt module, which keeps the domain free of any framework.

The classes under `agent/` are deliberately thin: they parse the agent's input, delegate to a use case, and map the result to a serializable type. No domain logic lives there.

## The dependency graph

`Task` does not store its dependencies. They are edges, `DependencyEdge(dependent, prerequisite)`, owned by the graph, which keeps both cycle detection and actionability as pure graph operations.

Before an edge is persisted, `DependencyGraph` checks whether the prerequisite can already reach the dependent. If it can, the edge would create a cycle and is refused. The reachability search is iterative, so deep chains do not overflow the stack and a graph that is already cyclic still terminates. Actionability treats an unknown prerequisite as not completed, so a dangling edge keeps a task blocked instead of silently releasing it.

Time comes from an injected `Clock` and ids from an injected generator, so overdue checks, recurrence, and id assignment are deterministic under test. The graph and use cases have 41 unit tests that run without a device.

## Exposed functions

The five functions live under `agent/` and cover the cases an app runs into when it becomes agent-callable:

- `getActionableTasks` — open tasks whose prerequisites are all completed.
- `getBlockingOverdueTasks` — overdue tasks that are blocking at least one other open task.
- `addTask` — creates a task, optionally linking prerequisites by id or by natural-language title.
- `completeTask` — completes a task and reports which dependents it unblocked.
- `deleteTask` — destructive, so it confirms first; the initial call only describes what would be removed.

Each function's KDoc is written as the description the agent reads. The annotation processor encodes it into the function metadata, which makes it the prompt that decides whether the agent picks the right tool, not documentation for a human.

## Building

Requires JDK 17, the Android SDK, and an API 36+ emulator or device with Google Play (AppFunctions needs Android 16 and Play services).

```
./gradlew :app:assembleDebug     # build the app
./gradlew :domain:test           # domain unit tests
./gradlew test                   # all unit tests
```

In Android Studio, open the project, start an API 36+ Play emulator, and run the `app` configuration (committed under `.run/`).

The current AndroidX libraries require AGP 9.1+ and compileSdk 37, so the project uses AGP 9.2.1, Gradle 9.4.1, Kotlin 2.3.21 and KSP 2.3.9, with compileSdk 37 and minSdk/targetSdk 36. AGP 9 ships built-in Kotlin and a new DSL that KSP does not yet support, so `gradle.properties` sets `android.builtInKotlin=false` and `android.newDsl=false` to keep the Kotlin-plus-KSP path. Both flags can be dropped once KSP supports built-in Kotlin.

## Invoking the functions

The on-device Gemini integration is currently in private preview, so the functions are driven through paths that work without it.

List what the system has indexed for the app:

```
adb shell cmd app_function list-app-functions \
  --package io.github.tonytonycoder11.agentictaskmanager
```

The output carries each function's id and its description (the KDoc), which is the quickest way to confirm the agent sees what was intended. To run one:

```
adb shell cmd app_function execute-app-function \
  --package io.github.tonytonycoder11.agentictaskmanager \
  --function io.github.tonytonycoder11.agentictaskmanager.agent.TaskQueryFunctions#getActionableTasks \
  --parameters '{}' --brief-yaml
```

The official [Testing Agent](https://github.com/android/appfunctions) discovers and runs the functions as well. From its `agent/` directory, `./run_privileged.sh --build` installs and launches it through an instrumentation that adopts shell privileges, so no rooted image is needed. Its retail flavour adds the natural-language path and takes an AI Studio Gemini key.

## Testing

The domain has unit tests for the graph (cycle detection and reachability), actionability, and each use case, including the cascade unlock and the recurrence date math. The data layer has tests for the entity-to-domain mappers. End-to-end execution is verified through `adb` and the Testing Agent. A harness under `tools/reliability-harness` measures how often an agent maps a natural-language request to the correct function and parameters, and how much the wording of the KDoc moves that number.

## Notes on some decisions

Enum-typed fields cross the agent boundary as documented strings (`"HIGH"`, `"WEEKLY"`), parsed case-insensitively with a default. This is more tolerant of imperfect agent input; whether a closed enum extracts more reliably is one of the things the harness measures.

The write use cases read, check, then write (load the graph, test for a cycle, persist). Two of them interleaving could each observe an acyclic graph and both commit, closing the cycle the check exists to prevent, so all writers share a single mutex. It matters once an agent and the UI issue calls at the same time.

`@AppFunction` and `AppFunctionConfiguration` live in `androidx.appfunctions.service`, while `AppFunctionContext` and the serializable annotations live in `androidx.appfunctions`. The documentation omits the sub-package, and the build error is how you find out.

## Status

The domain, Room persistence, the Compose UI, and the five AppFunctions are implemented and unit-tested. The functions are invocable over `adb` and through the official Testing Agent. The reliability harness is in place; collecting and publishing its numbers is the remaining work.

## License

Apache 2.0. See [LICENSE](LICENSE).
