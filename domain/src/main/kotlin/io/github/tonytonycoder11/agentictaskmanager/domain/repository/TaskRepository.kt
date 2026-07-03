package io.github.tonytonycoder11.agentictaskmanager.domain.repository

import io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import kotlinx.coroutines.flow.Flow

/**
 * The persistence boundary, expressed in domain terms only.
 *
 * Declared in `:domain` but implemented in `:app/data` (Room), keeping the domain free of Room,
 * SQLite and Android and unit-testable against an in-memory fake.
 */
interface TaskRepository {

    /** Re-emits the full task list on every change. */
    fun observeTasks(): Flow<List<Task>>

    /** Re-emits the full dependency edge list on every change. */
    fun observeDependencies(): Flow<List<DependencyEdge>>

    suspend fun getAllTasks(): List<Task>

    suspend fun getAllDependencies(): List<DependencyEdge>

    suspend fun getTask(id: TaskId): Task?

    suspend fun insertTask(task: Task)

    suspend fun updateTask(task: Task)

    /** Deletes a task; implementations MUST also remove any edge referencing it (no dangling edges). */
    suspend fun deleteTask(id: TaskId)

    suspend fun addDependency(edge: DependencyEdge)

    suspend fun removeDependency(edge: DependencyEdge)
}
