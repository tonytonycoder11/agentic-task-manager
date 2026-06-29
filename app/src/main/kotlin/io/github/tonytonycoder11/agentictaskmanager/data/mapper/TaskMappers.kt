package io.github.tonytonycoder11.agentictaskmanager.data.mapper

import io.github.tonytonycoder11.agentictaskmanager.data.entity.DependencyEntity
import io.github.tonytonycoder11.agentictaskmanager.data.entity.TaskEntity
import io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Priority
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Recurrence
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskStatus
import java.time.Instant

/**
 * Translation between Room rows and domain models. Keeping this in one place means the rest of
 * the data layer hands the domain clean [Task]/[DependencyEdge] objects and never leaks Room
 * types upward. Enum parsing is defensive: an unrecognised stored value falls back to a safe
 * default rather than crashing.
 */

fun TaskEntity.toDomain(): Task = Task(
    id = TaskId(id),
    title = title,
    description = description,
    priority = priority.toPriority(),
    dueAt = dueAtEpochMillis?.let(Instant::ofEpochMilli),
    status = status.toStatus(),
    recurrence = recurrence.toRecurrence(),
    parentId = parentId?.let(::TaskId),
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id.value,
    title = title,
    description = description,
    priority = priority.name,
    dueAtEpochMillis = dueAt?.toEpochMilli(),
    status = status.name,
    recurrence = recurrence.name,
    parentId = parentId?.value,
)

fun DependencyEntity.toDomain(): DependencyEdge =
    DependencyEdge(dependentId = TaskId(dependentId), prerequisiteId = TaskId(prerequisiteId))

fun DependencyEdge.toEntity(): DependencyEntity =
    DependencyEntity(dependentId = dependentId.value, prerequisiteId = prerequisiteId.value)

private fun String.toPriority(): Priority =
    runCatching { Priority.valueOf(this) }.getOrDefault(Priority.MEDIUM)

private fun String.toStatus(): TaskStatus =
    runCatching { TaskStatus.valueOf(this) }.getOrDefault(TaskStatus.OPEN)

private fun String.toRecurrence(): Recurrence =
    runCatching { Recurrence.valueOf(this) }.getOrDefault(Recurrence.NONE)
