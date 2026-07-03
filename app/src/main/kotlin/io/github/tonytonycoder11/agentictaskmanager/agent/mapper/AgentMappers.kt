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
 * Translation between agent DTOs and domain types, plus tolerant parsing of agent-supplied strings.
 * Parsing is forgiving where it safely can be (enums fall back to a default) and explicit where it
 * cannot (a bad date or blank id raises [AppFunctionInvalidArgumentException] rather than mishandling).
 */

/** Rich mapping from a graph insight; the normal path for query results. */
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

/** Mapping from a bare [Task] when the caller already knows its actionability. */
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
 * Parses an optional due date: an ISO-8601 instant, or a plain ISO date taken as start of day UTC.
 * Null stays null; anything else is an invalid argument.
 */
fun parseDueDate(value: String?): Instant? {
    val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    runCatching { return Instant.parse(raw) }
    runCatching { return LocalDate.parse(raw).atStartOfDay(ZoneOffset.UTC).toInstant() }
    throw AppFunctionInvalidArgumentException(
        "dueDate must be ISO-8601 (e.g. 2026-07-01 or 2026-07-01T09:00:00Z), got: $raw",
    )
}
