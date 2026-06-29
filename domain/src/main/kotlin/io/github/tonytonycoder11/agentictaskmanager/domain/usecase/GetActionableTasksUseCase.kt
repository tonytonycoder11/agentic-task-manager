package io.github.tonytonycoder11.agentictaskmanager.domain.usecase

import io.github.tonytonycoder11.agentictaskmanager.domain.graph.TaskInsight
import io.github.tonytonycoder11.agentictaskmanager.domain.graph.TaskInsights
import io.github.tonytonycoder11.agentictaskmanager.domain.repository.TaskRepository

/**
 * Returns the tasks the user can work on right now: OPEN tasks whose every dependency is already
 * completed (nothing is blocking them), ordered by urgency.
 *
 * This is the canonical "rich, read-only graph query" of the project — the one an agent will be
 * asked for as "what can I do now / what's unblocked / what should I focus on".
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
