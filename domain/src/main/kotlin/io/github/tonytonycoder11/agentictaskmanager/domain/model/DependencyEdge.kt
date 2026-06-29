package io.github.tonytonycoder11.agentictaskmanager.domain.model

/**
 * A directed dependency between two tasks.
 *
 * Read it as: **[dependentId] depends on [prerequisiteId]** — equivalently,
 * "[prerequisiteId] blocks [dependentId]". The dependent cannot be started until the
 * prerequisite is completed.
 *
 * The full set of edges forms a directed graph that MUST remain acyclic (a DAG): if it had a
 * cycle, a group of tasks would block each other forever and none could ever become
 * actionable. Cycle prevention lives in
 * [DependencyGraph][io.github.tonytonycoder11.agentictaskmanager.domain.graph.DependencyGraph].
 */
data class DependencyEdge(
    val dependentId: TaskId,
    val prerequisiteId: TaskId,
) {
    init {
        require(dependentId != prerequisiteId) {
            "A task cannot depend on itself ($dependentId)"
        }
    }
}
