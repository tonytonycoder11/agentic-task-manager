package io.github.tonytonycoder11.agentictaskmanager.domain.graph

import io.github.tonytonycoder11.agentictaskmanager.domain.model.Task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskStatus

/**
 * A read-model pairing a [Task] with the graph-derived facts about it, so actionability is
 * computed in exactly one place for every consumer (UI, query use cases, agent DTO mappers).
 *
 * @property isActionable whether it can be started right now (OPEN + all prerequisites done).
 * @property blockedBy prerequisites still blocking it (empty if unblocked).
 * @property blocks tasks that directly depend on it.
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
     * A [TaskInsight] for every task in [tasks], with relationships from [edges]. Status is read
     * from [tasks] themselves, so re-running after a completion reflects dependents' new state.
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
