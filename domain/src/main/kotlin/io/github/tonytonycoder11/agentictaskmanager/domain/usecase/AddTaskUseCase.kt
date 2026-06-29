package io.github.tonytonycoder11.agentictaskmanager.domain.usecase

import io.github.tonytonycoder11.agentictaskmanager.domain.graph.DependencyGraph
import io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Priority
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Recurrence
import io.github.tonytonycoder11.agentictaskmanager.domain.model.Task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.repository.TaskRepository
import io.github.tonytonycoder11.agentictaskmanager.domain.support.IdGenerator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

/**
 * Input for [AddTaskUseCase], expressed in pure domain types.
 *
 * Dependencies can be requested two ways, mirroring how an agent will call this in Phase 2:
 *  - [dependsOnTaskIds] — exact ids the caller already knows; and
 *  - [dependsOnTitles] — natural-language titles ("the slides task") that this use case
 *    resolves to ids by best match. Whatever cannot be resolved is reported back, never guessed.
 */
data class AddTaskCommand(
    val title: String,
    val description: String? = null,
    val priority: Priority = Priority.MEDIUM,
    val dueAt: Instant? = null,
    val dependsOnTaskIds: List<TaskId> = emptyList(),
    val dependsOnTitles: List<String> = emptyList(),
    val parentId: TaskId? = null,
    val recurrence: Recurrence = Recurrence.NONE,
)

/**
 * Result of [AddTaskUseCase].
 *
 * @property createdTask the task that was persisted.
 * @property linkedPrerequisites prerequisite ids that were successfully attached.
 * @property unresolvedDependencyTitles titles that matched zero or more-than-one task and so
 *   were left unlinked (the caller — or agent — should disambiguate and retry).
 * @property rejectedAsCycle prerequisite ids skipped because linking them would have created a
 *   cycle. For a brand-new task this is normally empty (a new task has no dependents yet), but
 *   it is reported for completeness.
 */
data class AddTaskResult(
    val createdTask: Task,
    val linkedPrerequisites: List<TaskId>,
    val unresolvedDependencyTitles: List<String>,
    val rejectedAsCycle: List<TaskId>,
)

/**
 * Creates a task and, optionally, links the tasks it depends on.
 *
 * Thin and deterministic: it resolves titles, guards every prospective dependency against
 * cycles and against unknown ids, persists the task and the accepted edges, then reports
 * exactly what it did and did not do.
 *
 * The whole create-and-link operation runs under the shared [mutationLock] so it cannot interleave
 * with other write use cases (all writes share one lock instance); this keeps the dependency graph
 * acyclic even under concurrent callers (UI taps today, agent calls in Phase 2).
 */
class AddTaskUseCase(
    private val repository: TaskRepository,
    private val idGenerator: IdGenerator,
    private val mutationLock: Mutex = Mutex(),
) {
    suspend operator fun invoke(command: AddTaskCommand): AddTaskResult = mutationLock.withLock {
        val existingTasks = repository.getAllTasks()

        // 1. Resolve any natural-language titles to concrete ids (or report them unresolved).
        val (resolvedFromTitles, unresolvedTitles) =
            resolveTitles(command.dependsOnTitles, existingTasks)

        // 2. Build the candidate prerequisite set: explicit ids + resolved titles, de-duplicated,
        //    keeping only ids that actually exist.
        val existingIds = existingTasks.mapTo(HashSet()) { it.id }
        val candidatePrerequisites = (command.dependsOnTaskIds + resolvedFromTitles)
            .distinct()
            .filter { it in existingIds }

        val newTask = Task(
            id = TaskId(idGenerator.newId()),
            title = command.title.trim(),
            description = command.description?.trim()?.ifBlank { null },
            priority = command.priority,
            dueAt = command.dueAt,
            recurrence = command.recurrence,
            parentId = command.parentId?.takeIf { it in existingIds },
        )

        // 3. Persist the task first so it becomes a real node, then add edges one by one,
        //    re-checking the cycle guard against the edges accepted so far.
        repository.insertTask(newTask)

        val workingEdges = repository.getAllDependencies().toMutableList()
        val linked = mutableListOf<TaskId>()
        val rejected = mutableListOf<TaskId>()
        for (prerequisite in candidatePrerequisites) {
            val graph = DependencyGraph.of(workingEdges)
            if (graph.wouldCreateCycle(dependent = newTask.id, prerequisite = prerequisite)) {
                rejected += prerequisite
            } else {
                val edge = DependencyEdge(dependentId = newTask.id, prerequisiteId = prerequisite)
                repository.addDependency(edge)
                workingEdges += edge
                linked += prerequisite
            }
        }

        AddTaskResult(
            createdTask = newTask,
            linkedPrerequisites = linked,
            unresolvedDependencyTitles = unresolvedTitles,
            rejectedAsCycle = rejected,
        )
    }

    /**
     * Resolves each requested title to a single task id, deterministically:
     *  1. a unique case-insensitive exact title match wins;
     *  2. otherwise a unique case-insensitive substring match wins;
     *  3. anything matching zero tasks or more than one is returned as unresolved.
     *
     * The use case never picks arbitrarily among ambiguous matches — surfacing the ambiguity is
     * the safe behaviour for an agent-facing API.
     */
    private fun resolveTitles(
        titles: List<String>,
        existingTasks: List<Task>,
    ): Pair<List<TaskId>, List<String>> {
        val resolved = mutableListOf<TaskId>()
        val unresolved = mutableListOf<String>()
        for (rawTitle in titles) {
            val query = rawTitle.trim()
            if (query.isEmpty()) {
                continue
            }
            val exact = existingTasks.filter { it.title.equals(query, ignoreCase = true) }
            val matches = when {
                exact.size == 1 -> exact
                exact.isNotEmpty() -> exact // multiple exact matches => ambiguous below
                else -> existingTasks.filter { it.title.contains(query, ignoreCase = true) }
            }
            if (matches.size == 1) {
                resolved += matches.first().id
            } else {
                unresolved += rawTitle
            }
        }
        return resolved to unresolved
    }
}
