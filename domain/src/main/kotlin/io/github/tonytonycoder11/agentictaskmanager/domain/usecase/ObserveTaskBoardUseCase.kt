package io.github.tonytonycoder11.agentictaskmanager.domain.usecase

import io.github.tonytonycoder11.agentictaskmanager.domain.graph.TaskInsight
import io.github.tonytonycoder11.agentictaskmanager.domain.graph.TaskInsights
import io.github.tonytonycoder11.agentictaskmanager.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Observes the whole task board as a live stream of [TaskInsight]s.
 *
 * It is the single reactive read the presentation layer needs: it `combine`s the task and
 * dependency streams and recomputes actionability/blockers on every change, so the UI updates the
 * instant a task is completed or a dependency is added. Keeping this in the domain means the
 * ViewModel depends only on use cases — never directly on the repository or the graph internals.
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
