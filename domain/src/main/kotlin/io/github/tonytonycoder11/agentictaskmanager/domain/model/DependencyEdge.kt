package io.github.tonytonycoder11.agentictaskmanager.domain.model

/**
 * A directed dependency: [dependentId] depends on [prerequisiteId], i.e. the dependent cannot
 * start until the prerequisite is completed.
 *
 * Edges must form a DAG — a cycle would block a group of tasks forever. Cycle prevention lives in
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
