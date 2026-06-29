package io.github.tonytonycoder11.agentictaskmanager.domain.usecase

import io.github.tonytonycoder11.agentictaskmanager.domain.fake.FakeTaskRepository
import io.github.tonytonycoder11.agentictaskmanager.domain.fake.task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddDependencyUseCaseTest {

    @Test
    fun `adds a valid dependency between existing tasks`() = runTest {
        val repo = FakeTaskRepository(initialTasks = listOf(task("A"), task("B")))
        val useCase = AddDependencyUseCase(repo)

        val result = useCase(dependentId = TaskId("B"), prerequisiteId = TaskId("A"))

        assertEquals(AddDependencyOutcome.ADDED, result.outcome)
        assertEquals(listOf(DependencyEdge(TaskId("B"), TaskId("A"))), repo.getAllDependencies())
    }

    @Test
    fun `rejects a self dependency`() = runTest {
        val repo = FakeTaskRepository(initialTasks = listOf(task("A")))
        val useCase = AddDependencyUseCase(repo)

        val result = useCase(dependentId = TaskId("A"), prerequisiteId = TaskId("A"))

        assertEquals(AddDependencyOutcome.SELF_DEPENDENCY, result.outcome)
    }

    @Test
    fun `rejects a dependency when a task is missing`() = runTest {
        val repo = FakeTaskRepository(initialTasks = listOf(task("A")))
        val useCase = AddDependencyUseCase(repo)

        val result = useCase(dependentId = TaskId("A"), prerequisiteId = TaskId("ghost"))

        assertEquals(AddDependencyOutcome.TASK_NOT_FOUND, result.outcome)
    }

    @Test
    fun `reports an already-existing dependency`() = runTest {
        val repo = FakeTaskRepository(
            initialTasks = listOf(task("A"), task("B")),
            initialEdges = listOf(DependencyEdge(TaskId("B"), TaskId("A"))),
        )
        val useCase = AddDependencyUseCase(repo)

        val result = useCase(dependentId = TaskId("B"), prerequisiteId = TaskId("A"))

        assertEquals(AddDependencyOutcome.ALREADY_EXISTS, result.outcome)
    }

    @Test
    fun `rejects a dependency that would create a cycle`() = runTest {
        // B already depends on A; making A depend on B would close a cycle.
        val repo = FakeTaskRepository(
            initialTasks = listOf(task("A"), task("B")),
            initialEdges = listOf(DependencyEdge(TaskId("B"), TaskId("A"))),
        )
        val useCase = AddDependencyUseCase(repo)

        val result = useCase(dependentId = TaskId("A"), prerequisiteId = TaskId("B"))

        assertEquals(AddDependencyOutcome.REJECTED_CYCLE, result.outcome)
        // The rejected edge must NOT have been persisted.
        assertEquals(listOf(DependencyEdge(TaskId("B"), TaskId("A"))), repo.getAllDependencies())
    }
}
