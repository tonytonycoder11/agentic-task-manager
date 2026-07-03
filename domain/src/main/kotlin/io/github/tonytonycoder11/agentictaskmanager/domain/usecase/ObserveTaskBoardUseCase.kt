package io.github.tonytonycoder11.agentictaskmanager.domain.usecase

import io.github.tonytonycoder11.agentictaskmanager.domain.graph.TaskInsight
import io.github.tonytonycoder11.agentictaskmanager.domain.graph.TaskInsights
import io.github.tonytonycoder11.agentictaskmanager.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Observes the whole task board as a live stream of [TaskInsight]s: combines the task and
 * dependency streams and recomputes actionability/blockers on every change. Keeping it in the
 * domain means the ViewModel depends only on use cases, not on the repository or graph internals.
 */
class ObserveTaskBoardUseCase(
    private val repository: TaskRepository,
) {
    operator fun invoke(): Flow<List<TaskInsight>> =
        combine(
            repository.observeTasks(),
            repository.observeDependencies(),
        ) { tasks, edges ->
            TaskInsights.computeAll(tasks, edges)
        }
}
