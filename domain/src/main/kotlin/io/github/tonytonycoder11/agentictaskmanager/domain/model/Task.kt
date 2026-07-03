package io.github.tonytonycoder11.agentictaskmanager.domain.model

import java.time.Instant

/**
 * A single task — the core entity of the domain.
 *
 * A [Task] does NOT hold its dependencies; those are modelled as [DependencyEdge]s owned by the
 * [graph][io.github.tonytonycoder11.agentictaskmanager.domain.graph.DependencyGraph], so cycle
 * detection and actionability stay pure graph problems. The one relationship stored on the task is
 * [parentId], since sub-tasks form a tree (one parent), not a general graph.
 *
 * @property status OPEN or COMPLETED; actionability is computed, not stored here.
 */
data class Task(
    val id: TaskId,
    val title: String,
    val description: String? = null,
    val priority: Priority = Priority.MEDIUM,
    val dueAt: Instant? = null,
    val status: TaskStatus = TaskStatus.OPEN,
    val recurrence: Recurrence = Recurrence.NONE,
    val parentId: TaskId? = null,
) {
    init {
        require(title.isNotBlank()) { "Task title must not be blank" }
        require(parentId != id) { "A task cannot be its own parent" }
    }

    val isOpen: Boolean get() = status == TaskStatus.OPEN
    val isCompleted: Boolean get() = status == TaskStatus.COMPLETED
}
