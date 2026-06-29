package io.github.tonytonycoder11.agentictaskmanager.domain.repository

import io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import kotlinx.coroutines.flow.Flow

/**
 * The persistence boundary, expressed in domain terms only.
 *
 * Declared here in `:domain` but implemented in `:app/data` (Room). The domain therefore knows
 * nothing about Room, SQLite or Android — it only knows it can read and write [Task]s and
 * [DependencyEdge]s. This is the seam that lets the whole domain be unit-tested against a
 * trivial in-memory fake.
 *
 * The `observe*` methods return cold [Flow]s for the reactive UI; the `suspend` methods are
 * one-shot reads/writes used by the use cases.
 */
interface TaskRepository {

    /** Emits the full task list and re-emits on every change. */
    fun observeTasks(): Flow<List<Task>>

    /** Emits the full dependency edge list and re-emits on every change. */
    fun observeDependencies(): Flow<List<DependencyEdge>>

    suspend fun getAllTasks(): List<Task>

    suspend fun getAllDependencies(): List<DependencyEdge>

    suspend fun getTask(id: TaskId): Task?

    suspend fun insertTask(task: Task)

    suspend fun updateTask(task: Task)

    /**
     * Deletes a single task. Implementations MUST also remove any dependency edge that
     * references it (the Room implementation relies on an ON DELETE CASCADE foreign key), so
     * that deleting a task can never leave a dangling edge behind.
     */
    suspend fun deleteTask(id: TaskId)

    suspend fun addDependency(edge: DependencyEdge)

    suspend fun removeDependency(edge: DependencyEdge)
}
