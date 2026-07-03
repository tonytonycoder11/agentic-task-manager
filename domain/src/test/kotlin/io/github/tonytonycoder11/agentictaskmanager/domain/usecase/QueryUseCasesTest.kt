package io.github.tonytonycoder11.agentictaskmanager.domain.usecase

import io.github.tonytonycoder11.agentictaskmanager.domain.fake.FIXED_NOW
import io.github.tonytonycoder11.agentictaskmanager.domain.fake.FakeTaskRepository
import io.github.tonytonycoder11.agentictaskmanager.domain.fake.fixedClock
import io.github.tonytonycoder11.agentictaskmanager.domain.fake.task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Priority
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.temporal.ChronoUnit

class QueryUseCasesTest {

    private val yesterday = FIXED_NOW.minus(1, ChronoUnit.DAYS)
    private val tomorrow = FIXED_NOW.plus(1, ChronoUnit.DAYS)

    @Test
    fun `getActionableTasks returns only unblocked open tasks, ordered by urgency`() = runTest {
        // A (done) blocks B; C free; D done. Expected: C (high) before B (medium).
        val repo = FakeTaskRepository(
            initialTasks = listOf(
                task("A", status = TaskStatus.COMPLETED),
                task("B", priority = Priority.MEDIUM),
                task("C", priority = Priority.HIGH),
                task("D", status = TaskStatus.COMPLETED),
            ),
            initialEdges = listOf(DependencyEdge(TaskId("B"), TaskId("A"))),
        )

        val actionable = GetActionableTasksUseCase(repo)().map { it.task.id }

        assertEquals(listOf(TaskId("C"), TaskId("B")), actionable)
    }

    @Test
    fun `getBlockingOverdue returns overdue tasks that block an open task`() = runTest {
        // Included only if overdue AND blocking an open task: A qualifies; C (blocks nobody) and D (not overdue) don't.
        val repo = FakeTaskRepository(
            initialTasks = listOf(
                task("A", dueAt = yesterday),
                task("B"),
                task("C", dueAt = yesterday),
                task("D", dueAt = tomorrow),
                task("E"),
            ),
            initialEdges = listOf(
                DependencyEdge(TaskId("B"), TaskId("A")),
                DependencyEdge(TaskId("E"), TaskId("D")),
            ),
        )

        val blocking = GetBlockingOverdueUseCase(repo, fixedClock())().map { it.task.id }

        assertEquals(listOf(TaskId("A")), blocking)
    }

    @Test
    fun `getBlockingOverdue excludes an overdue task whose dependents are all completed`() = runTest {
        // A is overdue but its only dependent B is completed, so A is no longer holding up work.
        val repo = FakeTaskRepository(
            initialTasks = listOf(
                task("A", dueAt = yesterday),
                task("B", status = TaskStatus.COMPLETED),
            ),
            initialEdges = listOf(DependencyEdge(TaskId("B"), TaskId("A"))),
        )

        val blocking = GetBlockingOverdueUseCase(repo, fixedClock())()

        assertEquals(emptyList<Any>(), blocking)
    }
}
