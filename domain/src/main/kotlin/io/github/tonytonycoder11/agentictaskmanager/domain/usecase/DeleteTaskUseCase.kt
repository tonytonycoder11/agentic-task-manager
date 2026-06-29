package io.github.tonytonycoder11.agentictaskmanager.domain.usecase

import io.github.tonytonycoder11.agentictaskmanager.domain.model.Task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.repository.TaskRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Outcome of a delete request. */
enum class DeleteOutcome {
    /** Nothing was deleted yet — the caller must confirm first (see [DeleteTaskUseCase]). */
    CONFIRMATION_REQUIRED,

    /** The task (and its sub-task subtree) was deleted. */
    DELETED,

    /** No task exists for the given id. */
    NOT_FOUND,
}

data class DeleteTaskResult(
    val outcome: DeleteOutcome,
    val affectedTaskIds: List<TaskId>,
    val message: String,
)

/**
 * Deletes a task — the project's one DESTRUCTIVE operation, so it is guarded by explicit,
 * two-step confirmation (security rule #5).
 *
 * Call it first with `confirmed = false`: nothing is deleted and the result lists exactly what
 * *would* be removed — the task plus its entire sub-task subtree (deletion cascades to children
 * so no orphaned sub-tasks are left behind). Only a second call with `confirmed = true` actually
 * deletes. Dependency edges referencing the deleted tasks are removed by the repository.
 *
 * Like the other write use cases it runs under the shared [mutationLock], so a deletion cannot
 * interleave with a concurrent add/complete and leave the graph in an inconsistent state.
 */
class DeleteTaskUseCase(
    private val repository: TaskRepository,
    private val mutationLock: Mutex = Mutex(),
) {
    suspend operator fun invoke(taskId: TaskId, confirmed: Boolean): DeleteTaskResult =
        mutationLock.withLock {
            val allTasks = repository.getAllTasks()
            val target = allTasks.firstOrNull { it.id == taskId }
                ?: return@withLock DeleteTaskResult(
                    outcome = DeleteOutcome.NOT_FOUND,
                    affectedTaskIds = emptyList(),
                    message = "No task found for id = ${taskId.value}.",
                )

            val affected = collectSubtree(taskId, allTasks)

            if (!confirmed) {
                val extra = affected.size - 1
                val detail = if (extra > 0) " and its $extra sub-task(s)" else ""
                return@withLock DeleteTaskResult(
                    outcome = DeleteOutcome.CONFIRMATION_REQUIRED,
                    affectedTaskIds = affected,
                    message = "This will permanently delete \"${target.title}\"$detail. " +
                        "Call again with confirmed = true to proceed.",
                )
            }

            // Delete children before parents so we never leave a dangling parent reference midway.
            affected.reversed().forEach { repository.deleteTask(it) }
            DeleteTaskResult(
                outcome = DeleteOutcome.DELETED,
                affectedTaskIds = affected,
                message = "Deleted ${affected.size} task(s).",
            )
        }

    /**
     * Returns [rootId] followed by every transitive sub-task (breadth-first). The order means
     * the root comes first and deeper descendants come later, so reversing the list yields a
     * safe leaf-to-root deletion order.
     */
    private fun collectSubtree(rootId: TaskId, allTasks: List<Task>): List<TaskId> {
        val childrenByParent: Map<TaskId, List<Task>> = allTasks
            .filter { it.parentId != null }
            .groupBy { it.parentId!! }

        val ordered = mutableListOf<TaskId>()
        val queue = ArrayDeque<TaskId>()
        queue.addLast(rootId)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            ordered += current
            childrenByParent[current].orEmpty().forEach { queue.addLast(it.id) }
        }
        return ordered
    }
}
