package io.github.tonytonycoder11.agentictaskmanager.domain.model

import java.time.Instant

/**
 * A single task — the core entity of the domain.
 *
 * Design choice that matters: a [Task] does NOT hold its dependencies. Dependencies are
 * modelled separately as [DependencyEdge]s and live in the [graph][io.github.tonytonycoder11.agentictaskmanager.domain.graph.DependencyGraph].
 * That keeps the entity small and makes the *graph* the single owner of all relationships,
 * which in turn makes cycle detection and actionability a pure graph problem.
 *
 * The only relationship stored directly on the task is [parentId], because sub-tasks form a
 * simple tree (a child has exactly one parent), not a general graph.
 *
 * @property id stable unique identifier.
 * @property title short human-readable name.
 * @property description optional longer text.
 * @property priority how urgent the task is.
 * @property dueAt optional deadline as an absolute instant (UTC); null means "no due date".
 * @property status OPEN or COMPLETED. Actionability is computed, not stored here.
 * @property recurrence whether/how the task repeats after completion.
 * @property parentId the parent task for a sub-task, or null for a top-level task.
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
