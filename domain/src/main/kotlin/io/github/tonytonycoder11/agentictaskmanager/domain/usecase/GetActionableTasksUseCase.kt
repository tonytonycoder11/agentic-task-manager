package io.github.tonytonycoder11.agentictaskmanager.domain.usecase

import io.github.tonytonycoder11.agentictaskmanager.domain.graph.TaskInsight
import io.github.tonytonycoder11.agentictaskmanager.domain.graph.TaskInsights
import io.github.tonytonycoder11.agentictaskmanager.domain.repository.TaskRepository

/**
 * Returns the tasks the user can work on right now: OPEN tasks whose every dependency is already
 * completed, ordered by urgency.
 */
class GetActionableTasksUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(): List<TaskInsight> {
        val tasks = repository.getAllTasks()
        val edges = repository.getAllDependencies()
        return TaskInsights.computeAll(tasks, edges)
            .filter { it.isActionable }
            .sortedWith(byUrgency)
    }
}
