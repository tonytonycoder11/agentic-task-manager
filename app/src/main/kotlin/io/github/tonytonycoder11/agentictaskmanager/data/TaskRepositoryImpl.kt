package io.github.tonytonycoder11.agentictaskmanager.data

import io.github.tonytonycoder11.agentictaskmanager.data.dao.DependencyDao
import io.github.tonytonycoder11.agentictaskmanager.data.dao.TaskDao
import io.github.tonytonycoder11.agentictaskmanager.data.mapper.toDomain
import io.github.tonytonycoder11.agentictaskmanager.data.mapper.toEntity
import io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation of the domain [TaskRepository].
 *
 * It is the only place that knows about Room. Every method maps between entities and domain
 * models so the domain stays Android-free. Deleting a task relies on the ON DELETE CASCADE
 * foreign keys to drop its dependency edges, honouring the repository contract.
 */
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val dependencyDao: DependencyDao,
) : TaskRepository {

    override fun observeTasks(): Flow<List<Task>> =
        taskDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeDependencies(): Flow<List<DependencyEdge>> =
        dependencyDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getAllTasks(): List<Task> =
        taskDao.getAll().map { it.toDomain() }

    override suspend fun getAllDependencies(): List<DependencyEdge> =
        dependencyDao.getAll().map { it.toDomain() }

    override suspend fun getTask(id: TaskId): Task? =
        taskDao.getById(id.value)?.toDomain()

    override suspend fun insertTask(task: Task) =
        taskDao.insert(task.toEntity())

    override suspend fun updateTask(task: Task) =
        taskDao.update(task.toEntity())

    override suspend fun deleteTask(id: TaskId) =
        taskDao.deleteById(id.value) // edges are removed by ON DELETE CASCADE

    override suspend fun addDependency(edge: DependencyEdge) =
        dependencyDao.insert(edge.toEntity())

    override suspend fun removeDependency(edge: DependencyEdge) =
        dependencyDao.delete(edge.dependentId.value, edge.prerequisiteId.value)
}
