package io.github.tonytonycoder11.agentictaskmanager.ui

import io.github.tonytonycoder11.agentictaskmanager.domain.model.Priority
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Recurrence
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskStatus

/** A single task as rendered in the list, with the graph-derived facts already resolved. */
data class TaskRowUi(
    val id: TaskId,
    val title: String,
    val description: String?,
    val priority: Priority,
    val status: TaskStatus,
    val isActionable: Boolean,
    val dueLabel: String?,
    val isOverdue: Boolean,
    val blockedByTitles: List<String>,
    val blocksCount: Int,
    val recurrence: Recurrence,
)

/** Lightweight option used to populate the dependency pickers in dialogs. */
data class TaskOptionUi(val id: TaskId, val title: String)

/** Immutable snapshot the screen renders. */
data class TaskListUiState(
    val rows: List<TaskRowUi> = emptyList(),
    val actionableCount: Int = 0,
    val totalCount: Int = 0,
    val pickerOptions: List<TaskOptionUi> = emptyList(),
)

/** Pending destructive deletion awaiting the user's explicit confirmation (two-step delete). */
data class PendingDeletion(val id: TaskId, val message: String)
