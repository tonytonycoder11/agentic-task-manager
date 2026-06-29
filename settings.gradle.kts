// Root Gradle settings.
//
// Two modules, with a deliberately hard boundary between them (see PLAN.md §4):
//   :domain  -> pure Kotlin/JVM, zero Android. Holds the entities, the dependency
//               graph logic (cycle detection + actionability) and the use cases.
//   :app     -> the Android application: Room persistence, Hilt DI, minimal Compose UI.
//               From Phase 2 it will also host the agent/ package (@AppFunction adapters).
//
// Keeping :domain as a JVM-only module means the Kotlin compiler itself forbids any
// accidental Android import inside the project's "crown jewel" — the graph logic.

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "agentic-task-manager"
include(":domain")
include(":app")
