package io.github.tonytonycoder11.agentictaskmanager.agent

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.service.AppFunction
import io.github.tonytonycoder11.agentictaskmanager.agent.dto.ActionableTasksResult
import io.github.tonytonycoder11.agentictaskmanager.agent.dto.BlockingOverdueResult
import io.github.tonytonycoder11.agentictaskmanager.agent.mapper.toDto
import io.github.tonytonycoder11.agentictaskmanager.di.IoDispatcher
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.GetActionableTasksUseCase
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.GetBlockingOverdueUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Read-only AppFunctions over the dependency graph. Thin adapters: each calls a domain use case and
 * maps the result to a DTO. Hilt constructs the class; the Application's AppFunctionConfiguration
 * hands the instance to the system. Work runs on an injected IO dispatcher, since AppFunctions
 * dispatch on the main thread by default.
 */
class TaskQueryFunctions @Inject constructor(
    private val getActionableTasksUseCase: GetActionableTasksUseCase,
    private val getBlockingOverdueUseCase: GetBlockingOverdueUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /**
     * Returns the tasks the user can work on right now: open tasks whose every dependency is already
     * completed, so nothing is blocking them, ordered by urgency. Use this whenever the user asks
     * what they can do now, what is unblocked, what is ready, or what to focus on next.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getActionableTasks(appFunctionContext: AppFunctionContext): ActionableTasksResult =
        withContext(ioDispatcher) {
            ActionableTasksResult(getActionableTasksUseCase().map { it.toDto() })
        }

    /**
     * Returns the highest-leverage problems: open tasks that are past their due date AND are still
     * blocking at least one other open task. These are both late and holding up the rest of the
     * plan. Use this for "what's overdue and stuck", "what's holding everything up", "what's late
     * and blocking other work".
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getBlockingOverdueTasks(appFunctionContext: AppFunctionContext): BlockingOverdueResult =
        withContext(ioDispatcher) {
            BlockingOverdueResult(getBlockingOverdueUseCase().map { it.toDto() })
        }
}
