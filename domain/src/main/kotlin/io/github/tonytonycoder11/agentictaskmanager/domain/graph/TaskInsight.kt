package io.github.tonytonycoder11.agentictaskmanager.domain.graph

import io.github.tonytonycoder11.agentictaskmanager.domain.model.Task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskStatus

/**
 * A read-model that pairs a [Task] with the graph-derived facts about it.
 *
 * This is the single projection consumed by everything that needs to *show* tasks: the
 * Compose UI, the query use cases, and (from Phase 2) the agent DTO mappers. Centralising it
 * here means actionability is computed in exactly one place.
 *
 * @property task the underlying task.
 * @property isActionable whether it can be started right now (OPEN + all prerequisites done).
 * @property blockedBy ids of the prerequisites still blocking it (empty if unblocked).
 * @property blocks ids of the tasks that directly depend on it (the tasks it is holding up).
 */
data class TaskInsight(
    val task: Task,
    val isActionable: Boolean,
    val blockedBy: List<TaskId>,
    val blocks: List<TaskId>,
)

/** Builds [TaskInsight]s for whole task lists, sharing one [DependencyGraph] per computation. */
object TaskInsights {

    /**
     * Computes a [TaskInsight] for every task in [tasks], using [edges] for the relationships.
     * The status lookup is backed by the tasks themselves, so completing a task and re-running
     * this immediately reflects the new actionability of its dependents.
     */
    fun computeAll(
        tasks: List<Task>,
        edges: List<io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge>,
    ): List<TaskInsight> {
        val graph = DependencyGraph.of(edges)
        val statusById: Map<TaskId, TaskStatus> = tasks.associate { it.id to it.status }
        val lookup = Actionability.StatusLookup { statusById[it] }
        return tasks.map { task ->
            TaskInsight(
                task = task,
                isActionable = Actionability.isActionable(task, graph, lookup),
                blockedBy = Actionability.blockedBy(task, graph, lookup),
                blocks = graph.dependentsOf(task.id).toList(),
            )
        }
    }
}
