package io.github.tonytonycoder11.agentictaskmanager.domain.usecase

import io.github.tonytonycoder11.agentictaskmanager.domain.graph.TaskInsight
import io.github.tonytonycoder11.agentictaskmanager.domain.graph.TaskInsights
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskStatus
import io.github.tonytonycoder11.agentictaskmanager.domain.repository.TaskRepository
import java.time.Clock

/**
 * Returns the highest-leverage problems: OPEN tasks that are past their due date AND are still
 * blocking at least one other OPEN task. These are both late and holding up the rest of the
 * plan, so they deserve attention first.
 *
 * "Now" comes from an injected [Clock], so overdue-ness is deterministic under test.
 */
class GetBlockingOverdueUseCase(
    private val repository: TaskRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): List<TaskInsight> {
        val now = clock.instant()
        val tasks = repository.getAllTasks()
        val edges = repository.getAllDependencies()
        val statusById = tasks.associate { it.id to it.status }

        return TaskInsights.computeAll(tasks, edges)
            .filter { insight ->
                val task = insight.task
                val isOverdue = task.dueAt != null && task.dueAt.isBefore(now)
                val blocksAnOpenTask = insight.blocks.any { statusById[it] == TaskStatus.OPEN }
                task.status == TaskStatus.OPEN && isOverdue && blocksAnOpenTask
            }
            .sortedWith(byUrgency)
    }
}
