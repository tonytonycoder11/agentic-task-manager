package io.github.tonytonycoder11.agentictaskmanager.domain.fake

import io.github.tonytonycoder11.agentictaskmanager.domain.model.Priority
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Recurrence
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskStatus
import io.github.tonytonycoder11.agentictaskmanager.domain.support.IdGenerator
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** A deterministic [IdGenerator] producing "t1", "t2", ... so generated ids are predictable. */
class SequentialIdGenerator(private val prefix: String = "t") : IdGenerator {
    private var counter = 0
    override fun newId(): String = "$prefix${++counter}"
}

/** A clock frozen at a known instant, for deterministic overdue/recurrence tests. */
val FIXED_NOW: Instant = Instant.parse("2026-06-28T12:00:00Z")

fun fixedClock(instant: Instant = FIXED_NOW): Clock = Clock.fixed(instant, ZoneOffset.UTC)

/** Concise task builder for tests; id doubles as the default title. */
fun task(
    id: String,
    title: String = id,
    status: TaskStatus = TaskStatus.OPEN,
    priority: Priority = Priority.MEDIUM,
    dueAt: Instant? = null,
    recurrence: Recurrence = Recurrence.NONE,
    parentId: String? = null,
): Task = Task(
    id = TaskId(id),
    title = title,
    status = status,
    priority = priority,
    dueAt = dueAt,
    recurrence = recurrence,
    parentId = parentId?.let { TaskId(it) },
)
