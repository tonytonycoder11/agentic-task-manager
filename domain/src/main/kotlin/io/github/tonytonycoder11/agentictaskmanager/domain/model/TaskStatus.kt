package io.github.tonytonycoder11.agentictaskmanager.domain.model

/**
 * Lifecycle state of a task: [OPEN] or [COMPLETED]. Actionability (whether an OPEN task can start
 * now) is not stored here — it depends on other tasks and is computed from the dependency graph.
 * See `graph/Actionability`.
 */
enum class TaskStatus {
    OPEN,
    COMPLETED,
}
