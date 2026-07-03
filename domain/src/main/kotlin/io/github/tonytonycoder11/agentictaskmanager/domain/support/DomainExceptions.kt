package io.github.tonytonycoder11.agentictaskmanager.domain.support

import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId

/**
 * Thrown when a use case operates on a task id that does not exist.
 *
 * A typed exception (rather than null) so the agent layer can translate it into a structured
 * `AppFunctionElementNotFoundException`.
 */
class TaskNotFoundException(val taskId: TaskId) :
    NoSuchElementException("No task found for id = ${taskId.value}")
