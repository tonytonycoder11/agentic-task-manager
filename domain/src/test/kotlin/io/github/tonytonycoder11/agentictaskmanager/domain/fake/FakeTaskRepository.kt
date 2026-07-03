package io.github.tonytonycoder11.agentictaskmanager.domain.fake

import io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory [TaskRepository] for unit tests, letting the domain be exercised without Room or Android.
 * Emulates Room's ON DELETE CASCADE: deleting a task also removes any edge that references it.
 */
class FakeTaskRepository(
    initialTasks: List<Task> = emptyList(),
    initialEdges: List<DependencyEdge> = emptyList(),
) : TaskRepository {

    private val tasksFlow = MutableStateFlow(initialTasks)
    private val edgesFlow = MutableStateFlow(initialEdges)

    override fun observeTasks(): StateFlow<List<Task>> = tasksFlow

    override fun observeDependencies(): StateFlow<List<DependencyEdge>> = edgesFlow

    override suspend fun getAllTasks(): List<Task> = tasksFlow.value

    override suspend fun getAllDependencies(): List<DependencyEdge> = edgesFlow.value

    override suspend fun getTask(id: TaskId): Task? = tasksFlow.value.firstOrNull { it.id == id }

    override suspend fun insertTask(task: Task) {
        tasksFlow.value = tasksFlow.value.filterNot { it.id == task.id } + task
    }

    override suspend fun updateTask(task: Task) {
        tasksFlow.value = tasksFlow.value.map { if (it.id == task.id) task else it }
    }

    override suspend fun deleteTask(id: TaskId) {
        tasksFlow.value = tasksFlow.value.filterNot { it.id == id }
        // ON DELETE CASCADE: drop edges touching the deleted task.
        edgesFlow.value = edgesFlow.value.filterNot {
            it.dependentId == id || it.prerequisiteId == id
        }
    }

    override suspend fun addDependency(edge: DependencyEdge) {
        if (edgesFlow.value.none { it == edge }) {
            edgesFlow.value = edgesFlow.value + edge
        }
    }

    override suspend fun removeDependency(edge: DependencyEdge) {
        edgesFlow.value = edgesFlow.value - edge
    }
}
