package io.github.tonytonycoder11.agentictaskmanager.agent

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.service.AppFunction
import io.github.tonytonycoder11.agentictaskmanager.agent.dto.ActionableTasksResult
import io.github.tonytonycoder11.agentictaskmanager.agent.dto.BlockingOverdueResult
import io.github.tonytonycoder11.agentictaskmanager.agent.mapper.toDto
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.GetActionableTasksUseCase
import io.github.tonytonycoder11.agentictaskmanager.domain.usecase.GetBlockingOverdueUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Read-only AppFunctions that traverse the dependency graph. These are thin adapters: each one just
 * calls the matching domain use case and maps the result to a DTO — there is no domain logic here.
 * Hilt builds this class (constructor injection); the Application's AppFunctionConfiguration hands
 * the instance to the system (see AtmApplication).
 *
 * AppFunctions run on the UI thread by default, so each one moves to [Dispatchers.IO] for the work.
 */
class TaskQueryFunctions @Inject constructor(
    private val getActionableTasksUseCase: GetActionableTasksUseCase,
    private val getBlockingOverdueUseCase: GetBlockingOverdueUseCase,
) {

    /**
     * Returns the tasks the user can work on right now: open tasks whose every dependency is already
     * completed, so nothing is blocking them, ordered by urgency. Use this whenever the user asks
     * what they can do now, what is unblocked, what is ready, or what to focus on next.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getActionableTasks(appFunctionContext: AppFunctionContext): ActionableTasksResult =
        withContext(Dispatchers.IO) {
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
        withContext(Dispatchers.IO) {
            BlockingOverdueResult(getBlockingOverdueUseCase().map { it.toDto() })
        }
}
