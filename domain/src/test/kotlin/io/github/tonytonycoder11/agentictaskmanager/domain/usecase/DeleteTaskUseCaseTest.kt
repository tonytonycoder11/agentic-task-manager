package io.github.tonytonycoder11.agentictaskmanager.domain.usecase

import io.github.tonytonycoder11.agentictaskmanager.domain.fake.FakeTaskRepository
import io.github.tonytonycoder11.agentictaskmanager.domain.fake.task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteTaskUseCaseTest {

    @Test
    fun `first call requires confirmation and deletes nothing`() = runTest {
        val repo = FakeTaskRepository(initialTasks = listOf(task("A")))
        val useCase = DeleteTaskUseCase(repo)

        val result = useCase(TaskId("A"), confirmed = false)

        assertEquals(DeleteOutcome.CONFIRMATION_REQUIRED, result.outcome)
        assertEquals(listOf(TaskId("A")), result.affectedTaskIds)
        assertEquals(1, repo.getAllTasks().size) // nothing actually deleted
    }

    @Test
    fun `confirmed delete removes the task and cascades its dependency edges`() = runTest {
        val repo = FakeTaskRepository(
            initialTasks = listOf(task("A"), task("B")),
            initialEdges = listOf(DependencyEdge(TaskId("B"), TaskId("A"))),
        )
        val useCase = DeleteTaskUseCase(repo)

        val result = useCase(TaskId("A"), confirmed = true)

        assertEquals(DeleteOutcome.DELETED, result.outcome)
        assertEquals(listOf(task("B")), repo.getAllTasks())
        assertTrue(repo.getAllDependencies().isEmpty()) // edge to A was cascaded away
    }

    @Test
    fun `confirmation summary and deletion cover the whole sub-task subtree`() = runTest {
        // parent -> child -> grandchild
        val repo = FakeTaskRepository(
            initialTasks = listOf(
                task("parent"),
                task("child", parentId = "parent"),
                task("grandchild", parentId = "child"),
            ),
        )
        val useCase = DeleteTaskUseCase(repo)

        val preview = useCase(TaskId("parent"), confirmed = false)
        assertEquals(
            setOf(TaskId("parent"), TaskId("child"), TaskId("grandchild")),
            preview.affectedTaskIds.toSet(),
        )

        val deleted = useCase(TaskId("parent"), confirmed = true)
        assertEquals(DeleteOutcome.DELETED, deleted.outcome)
        assertTrue(repo.getAllTasks().isEmpty())
    }

    @Test
    fun `deleting an unknown task reports NOT_FOUND`() = runTest {
        val repo = FakeTaskRepository()
        val useCase = DeleteTaskUseCase(repo)

        val result = useCase(TaskId("ghost"), confirmed = true)

        assertEquals(DeleteOutcome.NOT_FOUND, result.outcome)
    }
}
