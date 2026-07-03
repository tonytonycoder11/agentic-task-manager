package io.github.tonytonycoder11.agentictaskmanager.domain.graph

import io.github.tonytonycoder11.agentictaskmanager.domain.model.Task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskStatus

/**
 * A task is actionable iff it is OPEN and every prerequisite is COMPLETED.
 *
 * Actionability is derived, never stored, so it stays correct as tasks complete.
 */
object Actionability {

    /**
     * Status lookup for a task id. An unknown id (e.g. a dangling prerequisite) returns null and
     * is treated as "not completed", keeping the task blocked rather than silently unblocking it.
     */
    fun interface StatusLookup {
        fun statusOf(id: TaskId): TaskStatus?
    }

    /** True if [task] is OPEN and nothing still blocks it. */
    fun isActionable(task: Task, graph: DependencyGraph, status: StatusLookup): Boolean {
        if (task.status != TaskStatus.OPEN) return false
        return graph.prerequisitesOf(task.id).all { status.statusOf(it) == TaskStatus.COMPLETED }
    }

    /** Prerequisites of [task] not yet completed; empty means unblocked. */
    fun blockedBy(task: Task, graph: DependencyGraph, status: StatusLookup): List<TaskId> =
        graph.prerequisitesOf(task.id)
            .filter { status.statusOf(it) != TaskStatus.COMPLETED }
            .toList()
}
