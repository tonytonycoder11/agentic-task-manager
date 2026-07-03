package io.github.tonytonycoder11.agentictaskmanager.data.mapper

import io.github.tonytonycoder11.agentictaskmanager.data.entity.DependencyEntity
import io.github.tonytonycoder11.agentictaskmanager.data.entity.TaskEntity
import io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Priority
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Recurrence
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/** Plain-JVM tests for the Room <-> domain mappers; entities are plain data classes, so no Android runtime is needed. */
class TaskMappersTest {

    @Test
    fun `task round-trips through the entity losslessly`() {
        val task = Task(
            id = TaskId("t1"),
            title = "Prepare slides",
            description = "Draft the deck",
            priority = Priority.HIGH,
            dueAt = Instant.parse("2026-06-28T12:00:00Z"),
            status = TaskStatus.COMPLETED,
            recurrence = Recurrence.WEEKLY,
            parentId = TaskId("parent"),
        )

        assertEquals(task, task.toEntity().toDomain())
    }

    @Test
    fun `a task with null optional fields round-trips`() {
        val task = Task(id = TaskId("t2"), title = "Buy milk") // description/dueAt/parentId null

        val restored = task.toEntity().toDomain()

        assertEquals(task, restored)
        assertEquals(null, restored.dueAt)
        assertEquals(null, restored.parentId)
    }

    @Test
    fun `unknown stored enum values fall back to safe defaults instead of crashing`() {
        val corrupt = TaskEntity(
            id = "t3",
            title = "Legacy row",
            description = null,
            priority = "NOT_A_PRIORITY",
            dueAtEpochMillis = null,
            status = "NOT_A_STATUS",
            recurrence = "NOT_A_RECURRENCE",
            parentId = null,
        )

        val task = corrupt.toDomain()

        assertEquals(Priority.MEDIUM, task.priority)
        assertEquals(TaskStatus.OPEN, task.status)
        assertEquals(Recurrence.NONE, task.recurrence)
    }

    @Test
    fun `dependency edge round-trips through the entity`() {
        val edge = DependencyEdge(dependentId = TaskId("B"), prerequisiteId = TaskId("A"))

        assertEquals(edge, edge.toEntity().toDomain())
        assertEquals(DependencyEntity("B", "A"), edge.toEntity())
    }
}
