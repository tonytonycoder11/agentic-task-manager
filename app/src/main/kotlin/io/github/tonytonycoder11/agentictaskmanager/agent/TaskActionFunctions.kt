package io.github.tonytonycoder11.agentictaskmanager.agent

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.service.AppFunction
import io.github.tonytonycoder11.agentictaskmanager.agent.dto.AddTaskParams
import io.github.tonytonycoder11.agentictaskmanager.agent.dto.AddTaskResultDto
import io.github.tonytonycoder11.agentictaskmanager.agent.dto.CompleteTaskParams
import io.github.tonytonycoder11.agentictaskmanager.agent.dto.CompleteTaskResultDto
import io.github.tonytonycoder11.agentictaskmanager.agent.dto.DeleteTaskParams
import io.github.tonytonycoder11.agentictaskmanager.agent.dto.DeleteTaskResultDto
import io.github.tonytonycoder11.agentictaskmanager.agent.mapper.parseDueDate
import io.github.tonytonycoder11.agentictaskmanager.agent.mapper.toDto
import io.github.tonytonycoder11.agentictaskmanager.agent.mapper.toPriorityOrDefault
import io.github.tonytonycoder11.agentictaskmanager.agent.mapper.toRecurrenceOrDefault
import io.github.tonytonycoder11.agentictaskmanager.agent.mapper.toTaskIdOrInvalid
import io.github.tonytonycoder11.agentictaskmanager.di.IoDispatcher
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.support.TaskNotFoundException
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.AddTaskCommand
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.AddTaskUseCase
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.CompleteTaskUseCase
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.DeleteTaskUseCase
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.GetTaskInsightUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Write AppFunctions (create, complete, delete). Thin adapters over the domain use cases: parse agent
 * input, call the use case, map the result back. Failures surface as AppFunctions exceptions the agent
 * understands ([AppFunctionInvalidArgumentException], [AppFunctionElementNotFoundException]).
 */
class TaskActionFunctions @Inject constructor(
    private val addTaskUseCase: AddTaskUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val getTaskInsightUseCase: GetTaskInsightUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Creates a new task. Use this for any "add ...", "create a task ...", "remind me to ...",
     * "new task ..." request; put the task name in title. Optionally link prerequisites it must wait
     * for: pass their ids in dependsOnTaskIds, or natural-language names in dependsOnTitles (resolved
     * by best match; anything ambiguous is reported back in unresolvedDependencyTitles, never
     * guessed). Set priority, dueDate, or recurrence when the request mentions them. A link that
     * would create a cycle is rejected and reported in rejectedAsCycle.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun addTask(
        appFunctionContext: AppFunctionContext,
        params: AddTaskParams,
    ): AddTaskResultDto = withContext(ioDispatcher) {
        if (params.title.isBlank()) {
            throw AppFunctionInvalidArgumentException("title must not be blank")
        }
        val command = AddTaskCommand(
            title = params.title,
            description = params.description,
            priority = params.priority.toPriorityOrDefault(),
            dueAt = parseDueDate(params.dueDate),
            dependsOnTaskIds = params.dependsOnTaskIds.map { it.toTaskIdOrInvalid("dependsOnTaskIds") },
            dependsOnTitles = params.dependsOnTitles,
            parentId = params.parentTaskId?.takeIf { it.isNotBlank() }?.let { TaskId(it.trim()) },
            recurrence = params.recurrence.toRecurrenceOrDefault(),
        )
        val result = addTaskUseCase(command)
        // Report the new task with its graph-derived actionability.
        val createdDto = getTaskInsightUseCase(result.createdTask.id)?.toDto()
            ?: result.createdTask.toDto(isActionable = false)
        AddTaskResultDto(
            createdTask = createdDto,
            linkedPrerequisites = result.linkedPrerequisites.map { it.value },
            unresolvedDependencyTitles = result.unresolvedDependencyTitles,
            rejectedAsCycle = result.rejectedAsCycle.map { it.value },
        )
    }

    /**
     * Marks a task as completed and reports which previously-blocked tasks became actionable as a
     * result (the cascade-unlock effect). If the task recurs, the next occurrence is created and its
     * id returned. Use for "mark X done", "complete X", "I finished X". Completing an already
     * completed task is a no-op.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun completeTask(
        appFunctionContext: AppFunctionContext,
        params: CompleteTaskParams,
    ): CompleteTaskResultDto = withContext(ioDispatcher) {
        val id = params.taskId.toTaskIdOrInvalid()
        val result = try {
            completeTaskUseCase(id)
        } catch (e: TaskNotFoundException) {
            throw AppFunctionElementNotFoundException("No task found for id = ${params.taskId}")
        }
        CompleteTaskResultDto(
            completedTask = result.completedTask.toDto(isActionable = false),
            newlyActionableTasks = result.newlyActionable.map { it.toDto(isActionable = true) },
            spawnedRecurrenceTaskId = result.spawnedRecurrence?.id?.value,
        )
    }

    /**
     * Deletes a task. Always call this to handle any "delete X", "remove X", "get rid of X" request:
     * it is safe by default and two-step, so calling it is never destructive on its own. Pass
     * confirmed = false on the first call to get a summary of exactly what would be removed (the task
     * and any of its sub-tasks; nothing is deleted yet), then call again with confirmed = true once
     * the user agrees. Do not refuse — calling with confirmed = false IS the correct first step.
     * Returns NOT_FOUND if the id does not exist.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun deleteTask(
        appFunctionContext: AppFunctionContext,
        params: DeleteTaskParams,
    ): DeleteTaskResultDto = withContext(ioDispatcher) {
        val id = params.taskId.toTaskIdOrInvalid()
        val result = deleteTaskUseCase(id, params.confirmed)
        DeleteTaskResultDto(
            outcome = result.outcome.name,
            message = result.message,
            affectedTaskIds = result.affectedTaskIds.map { it.value },
        )
    }
}
