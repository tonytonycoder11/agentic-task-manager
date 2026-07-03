package io.github.tonytonycoder11.agentictaskmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.tonytonycoder11.agentictaskmanager.domain.graph.TaskInsight
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskStatus
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.AddDependencyUseCase
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.AddTaskCommand
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.AddTaskUseCase
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.CompleteTaskUseCase
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.DeleteOutcome
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.DeleteTaskUseCase
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.ObserveTaskBoardUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Drives the single Compose screen, adapting domain results into UI rows and user-facing messages.
 *
 * Clean-architecture boundary: depends ONLY on domain use cases, never on the repository, Room, or
 * graph internals. The live board comes from [ObserveTaskBoardUseCase]; every mutation goes through
 * an action use case.
 */
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val observeTaskBoard: ObserveTaskBoardUseCase,
    private val addTask: AddTaskUseCase,
    private val addDependency: AddDependencyUseCase,
    private val completeTask: CompleteTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<TaskListUiState> =
        observeTaskBoard()
            .map { insights -> buildState(insights) }
            .catch {
                // Keep the screen alive if the task stream fails, and surface it.
                messages.trySend("Couldn't load tasks.")
                emit(TaskListUiState())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = TaskListUiState(),
            )

    private val pendingDeletionState = MutableStateFlow<PendingDeletion?>(null)
    val pendingDeletion: StateFlow<PendingDeletion?> = pendingDeletionState.asStateFlow()

    // One-shot snackbar messages; UNLIMITED so a burst of results is never silently dropped.
    private val messages = Channel<String>(Channel.UNLIMITED)
    val messageFlow: Flow<String> = messages.receiveAsFlow()

    fun onAddTask(title: String, dependsOn: List<TaskId>) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val result = addTask(AddTaskCommand(title = title.trim(), dependsOnTaskIds = dependsOn))
            val linked = result.linkedPrerequisites.size
            messages.trySend(
                if (linked > 0) "Added \"${result.createdTask.title}\" with $linked prerequisite(s)."
                else "Added \"${result.createdTask.title}\".",
            )
        }
    }

    fun onAddDependency(dependentId: TaskId, prerequisiteId: TaskId) {
        viewModelScope.launch {
            // The result message carries the cycle-rejection outcome.
            val result = addDependency(dependentId, prerequisiteId)
            messages.trySend(result.message)
        }
    }

    fun onCompleteTask(id: TaskId) {
        viewModelScope.launch {
            val result = completeTask(id)
            val unblocked = result.newlyActionable.map { it.title }
            messages.trySend(
                if (unblocked.isEmpty()) "Completed \"${result.completedTask.title}\"."
                else "Completed \"${result.completedTask.title}\". Unblocked: ${unblocked.joinToString()}.",
            )
        }
    }

    /** First step of the destructive delete: ask the domain what would be removed, then confirm. */
    fun onRequestDelete(id: TaskId) {
        viewModelScope.launch {
            val preview = deleteTask(id, confirmed = false)
            if (preview.outcome == DeleteOutcome.CONFIRMATION_REQUIRED) {
                pendingDeletionState.value = PendingDeletion(id, preview.message)
            } else {
                messages.trySend(preview.message)
            }
        }
    }

    /** Second step: the user confirmed, so actually delete. */
    fun onConfirmDelete() {
        val pending = pendingDeletionState.value ?: return
        pendingDeletionState.value = null
        viewModelScope.launch {
            val result = deleteTask(pending.id, confirmed = true)
            messages.trySend(result.message)
        }
    }

    fun onCancelDelete() {
        pendingDeletionState.value = null
    }

    private fun buildState(insights: List<TaskInsight>): TaskListUiState {
        val now = clock.instant()
        val titleById = insights.associate { it.task.id to it.task.title }

        val rows = insights
            .sortedWith(rowOrder)
            .map { it.toRowUi(now, titleById) }

        return TaskListUiState(
            rows = rows,
            actionableCount = insights.count { it.isActionable },
            totalCount = insights.size,
            pickerOptions = insights
                .sortedBy { it.task.title.lowercase() }
                .map { TaskOptionUi(it.task.id, it.task.title) },
        )
    }

    private fun TaskInsight.toRowUi(now: Instant, titleById: Map<TaskId, String>): TaskRowUi {
        val due = task.dueAt
        return TaskRowUi(
            id = task.id,
            title = task.title,
            description = task.description,
            priority = task.priority,
            status = task.status,
            isActionable = isActionable,
            dueLabel = due?.let { DueDateFormatter.format(it) },
            isOverdue = due != null && due.isBefore(now) && task.status == TaskStatus.OPEN,
            blockedByTitles = blockedBy.mapNotNull { titleById[it] },
            blocksCount = blocks.size,
            recurrence = task.recurrence,
        )
    }

    private companion object {
        /** Open tasks before completed ones; within each, highest priority then earliest due. */
        val rowOrder: Comparator<TaskInsight> =
            compareBy<TaskInsight> { it.task.status == TaskStatus.COMPLETED }
                .thenByDescending { it.task.priority.ordinal }
                .thenBy { it.task.dueAt ?: Instant.MAX }
                .thenBy { it.task.title.lowercase() }
    }
}
