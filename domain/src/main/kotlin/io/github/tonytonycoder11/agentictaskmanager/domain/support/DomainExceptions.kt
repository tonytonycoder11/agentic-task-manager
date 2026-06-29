package io.github.tonytonycoder11.agentictaskmanager.domain.support

import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId

/**
 * Thrown when a use case is asked to operate on a task id that does not exist.
 *
 * The domain throws this typed exception rather than returning nulls everywhere; the agent
 * layer (Phase 2) will translate it into the appropriate `AppFunctionElementNotFoundException`
 * so the calling agent gets a clean, structured error instead of a crash.
 */
class TaskNotFoundException(val taskId: TaskId) :
    NoSuchElementException("No task found for id = ${taskId.value}")
