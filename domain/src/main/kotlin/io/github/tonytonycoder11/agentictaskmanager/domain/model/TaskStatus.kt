package io.github.tonytonycoder11.agentictaskmanager.domain.model

/**
 * Lifecycle state of a task.
 *
 * Kept intentionally binary for Phase 1: a task is either still to do ([OPEN]) or
 * finished ([COMPLETED]). "Actionability" (whether an OPEN task can be started right now)
 * is NOT a stored status — it is computed on the fly from the dependency graph, because it
 * depends on the state of *other* tasks. See `graph/Actionability`.
 */
enum class TaskStatus {
    OPEN,
    COMPLETED,
}
