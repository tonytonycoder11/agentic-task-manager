package io.github.tonytonycoder11.agentictaskmanager.agent.dto

import androidx.appfunctions.AppFunctionSerializable

/**
 * Data-transfer types crossing the agent <-> app boundary (they travel over IPC, so every composite
 * type is an [AppFunctionSerializable]). Their KDoc is NOT just documentation: with
 * `isDescribedByKDoc = true` it is encoded into the function metadata the agent reads to decide how
 * to fill parameters and interpret results. Hence each field is described in agent-facing terms.
 */

/** A task as exposed to an agent. */
@AppFunctionSerializable
data class TaskDto(
    /** Stable unique id of the task. */
    val id: String,
    /** Short human-readable title. */
    val title: String,
    /** Optional longer description, or null. */
    val description: String?,
    /** Priority, one of: LOW, MEDIUM, HIGH, URGENT. */
    val priority: String,
    /** Due date-time in ISO-8601 (e.g. "2026-07-01T09:00:00Z"), or null if none. */
    val dueDate: String?,
    /** Lifecycle status, one of: OPEN, COMPLETED. */
    val status: String,
    /** True if the task can be worked on right now (OPEN and every prerequisite COMPLETED). */
    val isActionable: Boolean,
    /** Ids of the tasks still blocking this one (empty if nothing blocks it). */
    val blockedByTaskIds: List<String>,
    /** Ids of the tasks that depend on this one (the tasks it is currently blocking). */
    val blocksTaskIds: List<String>,
)

/** Parameters for creating a task. Only [title] is required. */
@AppFunctionSerializable
data class AddTaskParams(
    /** The new task's title (required, non-blank). */
    val title: String,
    /** Optional longer description. */
    val description: String? = null,
    /** Optional priority, one of LOW, MEDIUM, HIGH, URGENT; defaults to MEDIUM. */
    val priority: String? = null,
    /** Optional due date-time in ISO-8601. */
    val dueDate: String? = null,
    /** Ids of existing tasks this task should depend on (must be completed before it can start). */
    val dependsOnTaskIds: List<String> = emptyList(),
    /** Natural-language titles of tasks to depend on; resolved by best match, unresolved ones are reported. */
    val dependsOnTitles: List<String> = emptyList(),
    /** Optional id of a parent task, to create this as a sub-task. */
    val parentTaskId: String? = null,
    /** Optional recurrence, one of NONE, DAILY, WEEKLY, MONTHLY; defaults to NONE. */
    val recurrence: String? = null,
)

/** Parameters identifying the task to complete. */
@AppFunctionSerializable
data class CompleteTaskParams(
    /** Id of the task to mark as completed. */
    val taskId: String,
)

/** Parameters for the two-step destructive delete. */
@AppFunctionSerializable
data class DeleteTaskParams(
    /** Id of the task to delete. */
    val taskId: String,
    /**
     * Must be false on the first call: the function then returns a CONFIRMATION_REQUIRED summary of
     * exactly what will be deleted. Call again with true only after the user has agreed.
     */
    val confirmed: Boolean = false,
)

/** Result of getActionableTasks: the tasks the user can work on right now. */
@AppFunctionSerializable
data class ActionableTasksResult(val tasks: List<TaskDto>)

/** Result of getBlockingOverdueTasks: overdue tasks that are blocking other open tasks. */
@AppFunctionSerializable
data class BlockingOverdueResult(val tasks: List<TaskDto>)

/** Result of addTask. */
@AppFunctionSerializable
data class AddTaskResultDto(
    /** The task that was created. */
    val createdTask: TaskDto,
    /** Prerequisite ids that were successfully linked. */
    val linkedPrerequisites: List<String>,
    /** Requested dependency titles that matched zero or more-than-one task (left unlinked). */
    val unresolvedDependencyTitles: List<String>,
    /** Prerequisite ids skipped because linking them would have created a cycle. */
    val rejectedAsCycle: List<String>,
)

/** Result of completeTask, including the cascade-unlock effect. */
@AppFunctionSerializable
data class CompleteTaskResultDto(
    /** The task after being completed. */
    val completedTask: TaskDto,
    /** Tasks that became actionable because this task was completed. */
    val newlyActionableTasks: List<TaskDto>,
    /** If the task was recurring, the id of the next occurrence that was created; otherwise null. */
    val spawnedRecurrenceTaskId: String?,
)

/** Result of deleteTask. */
@AppFunctionSerializable
data class DeleteTaskResultDto(
    /** One of: CONFIRMATION_REQUIRED, DELETED, NOT_FOUND. */
    val outcome: String,
    /** A human-readable summary of what happened or what will happen. */
    val message: String,
    /** Ids of the tasks affected (the task and any of its sub-tasks). */
    val affectedTaskIds: List<String>,
)
