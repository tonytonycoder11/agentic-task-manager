package io.github.tonytonycoder11.agentictaskmanager.domain.usecase

import io.github.tonytonycoder11.agentictaskmanager.domain.graph.TaskInsight
import io.github.tonytonycoder11.agentictaskmanager.domain.graph.TaskInsights
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.repository.TaskRepository

/**
 * Returns the [TaskInsight] (task + actionability/blockers) for a single task id, or null if no
 * such task exists. Used by the agent layer to report a freshly created/changed task with its
 * correct, graph-derived actionability without duplicating the projection logic.
 */
class GetTaskInsightUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(id: TaskId): TaskInsight? {
        val tasks = repository.getAllTasks()
        val edges = repository.getAllDependencies()
        return TaskInsights.computeAll(tasks, edges).firstOrNull { it.task.id == id }
    }
}
