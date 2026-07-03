package io.github.tonytonycoder11.agentictaskmanager.domain.usecase

import io.github.tonytonycoder11.agentictaskmanager.domain.fake.FIXED_NOW
import io.github.tonytonycoder11.agentictaskmanager.domain.fake.FakeTaskRepository
import io.github.tonytonycoder11.agentictaskmanager.domain.fake.SequentialIdGenerator
import io.github.tonytonycoder11.agentictaskmanager.domain.fake.fixedClock
import io.github.tonytonycoder11.agentictaskmanager.domain.fake.task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Recurrence
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskStatus
import io.github.tonytonycoder11.agentictaskmanager.domain.support.TaskNotFoundException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class CompleteTaskUseCaseTest {

    private fun useCase(repo: FakeTaskRepository) =
        CompleteTaskUseCase(repo, SequentialIdGenerator("gen"), fixedClock())

    @Test
    fun `completing a task marks it completed`() = runTest {
        val repo = FakeTaskRepository(initialTasks = listOf(task("A")))

        val result = useCase(repo)(TaskId("A"))

        assertEquals(TaskStatus.COMPLETED, result.completedTask.status)
        assertEquals(TaskStatus.COMPLETED, repo.getTask(TaskId("A"))!!.status)
    }

    @Test
    fun `completing the last blocker makes the dependent newly actionable`() = runTest {
        // B depends on A.
        val repo = FakeTaskRepository(
            initialTasks = listOf(task("A"), task("B")),
            initialEdges = listOf(DependencyEdge(TaskId("B"), TaskId("A"))),
        )

        val result = useCase(repo)(TaskId("A"))

        assertEquals(listOf(TaskId("B")), result.newlyActionable.map { it.id })
    }

    @Test
    fun `a dependent with another open blocker does NOT become actionable`() = runTest {
        // C depends on both A and B; completing only A leaves it blocked by B.
        val repo = FakeTaskRepository(
            initialTasks = listOf(task("A"), task("B"), task("C")),
            initialEdges = listOf(
                DependencyEdge(TaskId("C"), TaskId("A")),
                DependencyEdge(TaskId("C"), TaskId("B")),
            ),
        )

        val result = useCase(repo)(TaskId("A"))

        assertTrue(result.newlyActionable.isEmpty())
    }

    @Test
    fun `completing a non-recurring task spawns nothing`() = runTest {
        val repo = FakeTaskRepository(initialTasks = listOf(task("A")))

        val result = useCase(repo)(TaskId("A"))

        assertNull(result.spawnedRecurrence)
        assertEquals(1, repo.getAllTasks().size)
    }

    @Test
    fun `completing a daily recurring task spawns the next occurrence one day later`() = runTest {
        val due = FIXED_NOW
        val repo = FakeTaskRepository(
            initialTasks = listOf(task("A", dueAt = due, recurrence = Recurrence.DAILY)),
        )

        val result = useCase(repo)(TaskId("A"))

        val spawned = result.spawnedRecurrence!!
        assertEquals(TaskStatus.OPEN, spawned.status)
        assertEquals(Recurrence.DAILY, spawned.recurrence)
        assertEquals(due.plus(1, ChronoUnit.DAYS), spawned.dueAt)
        // Completed original plus the new occurrence.
        assertEquals(2, repo.getAllTasks().size)
    }

    @Test
    fun `completing a weekly recurring task advances the due date by seven days`() = runTest {
        val due = FIXED_NOW
        val repo = FakeTaskRepository(
            initialTasks = listOf(task("A", dueAt = due, recurrence = Recurrence.WEEKLY)),
        )

        val spawned = useCase(repo)(TaskId("A")).spawnedRecurrence!!

        assertEquals(Recurrence.WEEKLY, spawned.recurrence)
        assertEquals(due.plus(7, ChronoUnit.DAYS), spawned.dueAt)
    }

    @Test
    fun `completing a monthly recurring task advances by one calendar month`() = runTest {
        // 2026-06-28 -> 2026-07-28.
        val due = Instant.parse("2026-06-28T12:00:00Z")
        val repo = FakeTaskRepository(
            initialTasks = listOf(task("A", dueAt = due, recurrence = Recurrence.MONTHLY)),
        )

        val spawned = useCase(repo)(TaskId("A")).spawnedRecurrence!!

        assertEquals(Instant.parse("2026-07-28T12:00:00Z"), spawned.dueAt)
    }

    @Test
    fun `monthly recurrence clamps month-end dates (Jan 31 to Feb 28)`() = runTest {
        // plusMonths clamps Jan 31 -> Feb 28 (2026 is not a leap year).
        val due = Instant.parse("2026-01-31T12:00:00Z")
        val repo = FakeTaskRepository(
            initialTasks = listOf(task("A", dueAt = due, recurrence = Recurrence.MONTHLY)),
        )

        val spawned = useCase(repo)(TaskId("A")).spawnedRecurrence!!

        assertEquals(Instant.parse("2026-02-28T12:00:00Z"), spawned.dueAt)
    }

    @Test
    fun `a recurring task with no due date advances from the clock`() = runTest {
        val repo = FakeTaskRepository(
            initialTasks = listOf(task("A", dueAt = null, recurrence = Recurrence.DAILY)),
        )

        val spawned = useCase(repo)(TaskId("A")).spawnedRecurrence!!

        assertEquals(FIXED_NOW.plus(1, ChronoUnit.DAYS), spawned.dueAt)
    }

    @Test
    fun `completing an already-completed recurring task is a no-op (idempotent)`() = runTest {
        val repo = FakeTaskRepository(
            initialTasks = listOf(task("A", dueAt = FIXED_NOW, recurrence = Recurrence.DAILY)),
        )
        val completeTask = useCase(repo)

        completeTask(TaskId("A")) // first completion spawns one occurrence
        val second = completeTask(TaskId("A")) // re-completing must not spawn another

        assertNull(second.spawnedRecurrence)
        assertTrue(second.newlyActionable.isEmpty())
        assertEquals(2, repo.getAllTasks().size)
    }

    @Test
    fun `completing an unknown task throws`() = runTest {
        val repo = FakeTaskRepository()

        val thrown = runCatching { useCase(repo)(TaskId("ghost")) }.exceptionOrNull()

        assertTrue(thrown is TaskNotFoundException)
    }
}
