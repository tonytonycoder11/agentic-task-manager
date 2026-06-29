package io.github.tonytonycoder11.agentictaskmanager.domain.usecase

import io.github.tonytonycoder11.agentictaskmanager.domain.graph.Actionability
import io.github.tonytonycoder11.agentictaskmanager.domain.graph.DependencyGraph
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Recurrence
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskStatus
import io.github.tonytonycoder11.agentictaskmanager.domain.repository.TaskRepository
import io.github.tonytonycoder11.agentictaskmanager.domain.support.IdGenerator
import io.github.tonytonycoder11.agentictaskmanager.domain.support.TaskNotFoundException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.ZoneOffset

/**
 * Result of completing a task.
 *
 * @property completedTask the task after being marked COMPLETED.
 * @property newlyActionable tasks that were blocked and are now actionable *because* this task
 *   was completed — this is the cascade-unlock effect, and a great thing to show to an agent.
 * @property spawnedRecurrence the next occurrence created for a recurring task, or null.
 */
data class CompleteTaskResult(
    val completedTask: Task,
    val newlyActionable: List<Task>,
    val spawnedRecurrence: Task?,
)

/**
 * Marks a task as completed and reports the ripple effects.
 *
 * Two interesting behaviours live here:
 *  1. **Cascade unlock** — after completing the task, any dependent whose last blocking
 *     prerequisite was this task becomes actionable; those are returned in [CompleteTaskResult].
 *  2. **Recurrence** — completing a recurring task spawns its next occurrence with the due date
 *     advanced by the recurrence period (calendar math done in UTC for determinism).
 *
 * The whole operation runs under the shared [mutationLock] so it cannot interleave with other
 * write use cases (see [AddDependencyUseCase] for why that matters), and it is **idempotent**:
 * completing an already-completed task is a no-op, so a double-tap or an agent retry never
 * re-runs the side effects (it would otherwise spawn a duplicate recurrence each time).
 *
 * Time is taken from an injected [Clock] so the behaviour is fully deterministic under test.
 */
class CompleteTaskUseCase(
    private val repository: TaskRepository,
    private val idGenerator: IdGenerator,
    private val clock: Clock,
    private val mutationLock: Mutex = Mutex(),
) {
    suspend operator fun invoke(taskId: TaskId): CompleteTaskResult = mutationLock.withLock {
        val task = repository.getTask(taskId) ?: throw TaskNotFoundException(taskId)

        // Idempotency guard: re-completing a finished task does nothing. Without it a second call
        // (fast double-tap, or an agent retrying) would spawn another recurrence occurrence.
        if (task.status == TaskStatus.COMPLETED) {
            return@withLock CompleteTaskResult(
                completedTask = task,
                newlyActionable = emptyList(),
                spawnedRecurrence = null,
            )
        }

        val completed = task.copy(status = TaskStatus.COMPLETED)
        repository.updateTask(completed)

        // Spawn the next occurrence for recurring tasks.
        val spawned = nextOccurrenceOf(task)?.also { repository.insertTask(it) }

        // Recompute actionability for the dependents of the just-completed task.
        val tasks = repository.getAllTasks()
        val edges = repository.getAllDependencies()
        val graph = DependencyGraph.of(edges)
        val statusById = tasks.associate { it.id to it.status }
        val lookup = Actionability.StatusLookup { statusById[it] }

        val newlyActionable = graph.dependentsOf(taskId)
            .mapNotNull { dependentId -> tasks.firstOrNull { it.id == dependentId } }
            .filter { Actionability.isActionable(it, graph, lookup) }

        CompleteTaskResult(
            completedTask = completed,
            newlyActionable = newlyActionable,
            spawnedRecurrence = spawned,
        )
    }

    /**
     * Builds the next occurrence of a recurring [task], or null if it does not recur.
     * The new occurrence is a fresh OPEN task; it deliberately does NOT carry over the original
     * task's dependencies (a repeating reminder starts unblocked). Its due date is advanced from
     * the original due date, or from "now" if the original had none.
     */
    private fun nextOccurrenceOf(task: Task): Task? {
        if (task.recurrence == Recurrence.NONE) return null
        val base = task.dueAt ?: clock.instant()
        val nextDue = base.atZone(ZoneOffset.UTC).let { zoned ->
            when (task.recurrence) {
                Recurrence.DAILY -> zoned.plusDays(1)
                Recurrence.WEEKLY -> zoned.plusWeeks(1)
                Recurrence.MONTHLY -> zoned.plusMonths(1)
                Recurrence.NONE -> return null
            }
        }.toInstant()

        return task.copy(
            id = TaskId(idGenerator.newId()),
            status = TaskStatus.OPEN,
            dueAt = nextDue,
        )
    }
}
