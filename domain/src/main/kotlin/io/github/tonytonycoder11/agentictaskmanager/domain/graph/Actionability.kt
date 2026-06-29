package io.github.tonytonycoder11.agentictaskmanager.domain.graph

import io.github.tonytonycoder11.agentictaskmanager.domain.model.Task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskStatus

/**
 * The rule at the centre of the whole product: **a task is _actionable_ if and only if it is
 * OPEN and every one of its prerequisites is COMPLETED.**
 *
 * Actionability is never stored — it is derived from the graph plus the current status of
 * other tasks, so it stays correct automatically as tasks are completed. These helpers are
 * pure functions over a [DependencyGraph] and a status lookup.
 */
object Actionability {

    /**
     * Status lookup for a task id. Returns null if the id is unknown (e.g. a dangling
     * prerequisite), which we deliberately treat as "not completed" — an unknown prerequisite
     * keeps a task blocked rather than silently unblocking it.
     */
    fun interface StatusLookup {
        fun statusOf(id: TaskId): TaskStatus?
    }

    /** True if [task] can be worked on right now: it is OPEN and nothing still blocks it. */
    fun isActionable(task: Task, graph: DependencyGraph, status: StatusLookup): Boolean {
        if (task.status != TaskStatus.OPEN) return false
        return graph.prerequisitesOf(task.id).all { status.statusOf(it) == TaskStatus.COMPLETED }
    }

    /**
     * The prerequisites of [task] that are NOT yet completed — i.e. exactly the tasks still
     * blocking it. Empty list means the task is unblocked.
     */
    fun blockedBy(task: Task, graph: DependencyGraph, status: StatusLookup): List<TaskId> =
        graph.prerequisitesOf(task.id)
            .filter { status.statusOf(it) != TaskStatus.COMPLETED }
            .toList()
}
