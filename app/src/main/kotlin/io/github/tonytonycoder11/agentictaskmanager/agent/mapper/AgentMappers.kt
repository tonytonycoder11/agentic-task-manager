package io.github.tonytonycoder11.agentictaskmanager.agent.mapper

import androidx.appfunctions.AppFunctionInvalidArgumentException
import io.github.tonytonycoder11.agentictaskmanager.agent.dto.TaskDto
import io.github.tonytonycoder11.agentictaskmanager.domain.graph.TaskInsight
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Priority
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Recurrence
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Translation between the agent DTOs and domain types, plus tolerant parsing of agent-supplied
 * strings. Agents pass imperfect input, so parsing is forgiving where it safely can be (enums are
 * case-insensitive and fall back to a default) and explicit where it cannot (an unparseable date or
 * a blank id is reported as an [AppFunctionInvalidArgumentException] rather than silently mishandled).
 */

/** Rich mapping from a graph insight (the normal path for query results). */
fun TaskInsight.toDto(): TaskDto = TaskDto(
    id = task.id.value,
    title = task.title,
    description = task.description,
    priority = task.priority.name,
    dueDate = task.dueAt?.toString(),
    status = task.status.name,
    isActionable = isActionable,
    blockedByTaskIds = blockedBy.map { it.value },
    blocksTaskIds = blocks.map { it.value },
)

/**
 * Mapping from a bare [Task] when the actionability is already known by the caller (e.g. a just
 * completed task is not actionable; a freshly unblocked task is).
 */
fun Task.toDto(
    isActionable: Boolean,
    blockedBy: List<TaskId> = emptyList(),
    blocks: List<TaskId> = emptyList(),
): TaskDto = TaskDto(
    id = id.value,
    title = title,
    description = description,
    priority = priority.name,
    dueDate = dueAt?.toString(),
    status = status.name,
    isActionable = isActionable,
    blockedByTaskIds = blockedBy.map { it.value },
    blocksTaskIds = blocks.map { it.value },
)

/** Parses a non-blank task id, or reports a clean invalid-argument error to the agent. */
fun String.toTaskIdOrInvalid(field: String = "taskId"): TaskId {
    if (isBlank()) throw AppFunctionInvalidArgumentException("$field must not be blank")
    return TaskId(trim())
}

/** Case-insensitive priority parsing; null or unrecognised falls back to MEDIUM. */
fun String?.toPriorityOrDefault(): Priority =
    this?.trim()?.uppercase()?.let { runCatching { Priority.valueOf(it) }.getOrNull() } ?: Priority.MEDIUM

/** Case-insensitive recurrence parsing; null or unrecognised falls back to NONE. */
fun String?.toRecurrenceOrDefault(): Recurrence =
    this?.trim()?.uppercase()?.let { runCatching { Recurrence.valueOf(it) }.getOrNull() } ?: Recurrence.NONE

/**
 * Parses an optional due date. Accepts a full ISO-8601 instant ("2026-07-01T09:00:00Z") or a plain
 * ISO date ("2026-07-01", interpreted as start of day UTC). Null stays null; anything else is an
 * invalid argument the agent should correct.
 */
fun parseDueDate(value: String?): Instant? {
    val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    runCatching { return Instant.parse(raw) }
    runCatching { return LocalDate.parse(raw).atStartOfDay(ZoneOffset.UTC).toInstant() }
    throw AppFunctionInvalidArgumentException(
        "dueDate must be ISO-8601 (e.g. 2026-07-01 or 2026-07-01T09:00:00Z), got: $raw",
    )
}
