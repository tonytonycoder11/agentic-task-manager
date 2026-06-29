package io.github.tonytonycoder11.agentictaskmanager.domain.usecase

import io.github.tonytonycoder11.agentictaskmanager.domain.fake.FakeTaskRepository
import io.github.tonytonycoder11.agentictaskmanager.domain.fake.SequentialIdGenerator
import io.github.tonytonycoder11.agentictaskmanager.domain.fake.task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddTaskUseCaseTest {

    @Test
    fun `creates a task with a generated id`() = runTest {
        val repo = FakeTaskRepository()
        val useCase = AddTaskUseCase(repo, SequentialIdGenerator())

        val result = useCase(AddTaskCommand(title = "Buy milk"))

        assertEquals(TaskId("t1"), result.createdTask.id)
        assertEquals("Buy milk", result.createdTask.title)
        assertEquals(listOf(result.createdTask), repo.getAllTasks())
    }

    @Test
    fun `links a dependency requested by id`() = runTest {
        val repo = FakeTaskRepository(initialTasks = listOf(task("A")))
        val useCase = AddTaskUseCase(repo, SequentialIdGenerator())

        val result = useCase(AddTaskCommand(title = "B", dependsOnTaskIds = listOf(TaskId("A"))))

        assertEquals(listOf(TaskId("A")), result.linkedPrerequisites)
        assertEquals(
            listOf(DependencyEdge(result.createdTask.id, TaskId("A"))),
            repo.getAllDependencies(),
        )
    }

    @Test
    fun `resolves a dependency requested by natural-language title`() = runTest {
        val repo = FakeTaskRepository(initialTasks = listOf(task("A", title = "Prepare slides")))
        val useCase = AddTaskUseCase(repo, SequentialIdGenerator())

        // Substring, different case — should still resolve uniquely to task A.
        val result = useCase(AddTaskCommand(title = "Present", dependsOnTitles = listOf("slides")))

        assertEquals(listOf(TaskId("A")), result.linkedPrerequisites)
        assertTrue(result.unresolvedDependencyTitles.isEmpty())
    }

    @Test
    fun `reports an ambiguous title as unresolved instead of guessing`() = runTest {
        val repo = FakeTaskRepository(
            initialTasks = listOf(task("A", title = "Email Bob"), task("B", title = "Email Alice")),
        )
        val useCase = AddTaskUseCase(repo, SequentialIdGenerator())

        val result = useCase(AddTaskCommand(title = "Follow up", dependsOnTitles = listOf("Email")))

        assertTrue(result.linkedPrerequisites.isEmpty())
        assertEquals(listOf("Email"), result.unresolvedDependencyTitles)
    }

    @Test
    fun `ignores a dependency id that does not exist`() = runTest {
        val repo = FakeTaskRepository()
        val useCase = AddTaskUseCase(repo, SequentialIdGenerator())

        val result = useCase(AddTaskCommand(title = "B", dependsOnTaskIds = listOf(TaskId("ghost"))))

        assertTrue(result.linkedPrerequisites.isEmpty())
        assertTrue(repo.getAllDependencies().isEmpty())
    }
}
