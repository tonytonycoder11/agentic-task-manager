package io.github.tonytonycoder11.agentictaskmanager.domain.usecase

import io.github.tonytonycoder11.agentictaskmanager.domain.graph.DependencyGraph
import io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.repository.TaskRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Why a dependency request did or did not result in a new edge. */
enum class AddDependencyOutcome {
    /** The edge was created. */
    ADDED,

    /** A task cannot depend on itself. */
    SELF_DEPENDENCY,

    /** One of the two task ids does not exist. */
    TASK_NOT_FOUND,

    /** The edge already existed; nothing changed. */
    ALREADY_EXISTS,

    /** Rejected because it would have introduced a cycle (the graph must stay acyclic). */
    REJECTED_CYCLE,
}

data class AddDependencyResult(
    val outcome: AddDependencyOutcome,
    val message: String,
)

/**
 * Links two EXISTING tasks: makes [dependentId] depend on [prerequisiteId].
 *
 * This is the use case where cycle detection truly earns its place — unlike adding a brand-new
 * task, linking two existing tasks can close a loop. Every rejection reason is reported as a
 * structured [AddDependencyOutcome] rather than thrown, because "you can't add that, it would
 * create a cycle" is a normal, expected answer for both the UI and an agent.
 *
 * The read-check-write (load the graph, test [DependencyGraph.wouldCreateCycle], then persist) runs
 * under the shared [mutationLock]. That serialization is what makes the acyclicity invariant
 * actually hold: without it, two concurrent additions could each independently observe an acyclic
 * graph and both persist, together closing the very cycle the guard exists to prevent. All write
 * use cases share the same lock instance.
 */
class AddDependencyUseCase(
    private val repository: TaskRepository,
    private val mutationLock: Mutex = Mutex(),
) {
    suspend operator fun invoke(
        dependentId: TaskId,
        prerequisiteId: TaskId,
    ): AddDependencyResult = mutationLock.withLock {
        if (dependentId == prerequisiteId) {
            return@withLock AddDependencyResult(
                AddDependencyOutcome.SELF_DEPENDENCY,
                "A task cannot depend on itself.",
            )
        }

        val tasks = repository.getAllTasks()
        val ids = tasks.mapTo(HashSet()) { it.id }
        if (dependentId !in ids || prerequisiteId !in ids) {
            return@withLock AddDependencyResult(
                AddDependencyOutcome.TASK_NOT_FOUND,
                "Both tasks must exist before they can be linked.",
            )
        }

        val edges = repository.getAllDependencies()
        if (edges.any { it.dependentId == dependentId && it.prerequisiteId == prerequisiteId }) {
            return@withLock AddDependencyResult(
                AddDependencyOutcome.ALREADY_EXISTS,
                "That dependency already exists.",
            )
        }

        val graph = DependencyGraph.of(edges)
        if (graph.wouldCreateCycle(dependent = dependentId, prerequisite = prerequisiteId)) {
            return@withLock AddDependencyResult(
                AddDependencyOutcome.REJECTED_CYCLE,
                "Rejected: this dependency would create a cycle.",
            )
        }

        repository.addDependency(DependencyEdge(dependentId, prerequisiteId))
        AddDependencyResult(AddDependencyOutcome.ADDED, "Dependency added.")
    }
}
