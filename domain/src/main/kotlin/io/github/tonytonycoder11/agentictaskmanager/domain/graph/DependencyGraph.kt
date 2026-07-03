package io.github.tonytonycoder11.agentictaskmanager.domain.graph

import io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId

/**
 * An immutable, side-effect-free view over the task dependency relationships, built once from a
 * flat list of [DependencyEdge]s.
 *
 * Edge direction convention (see [DependencyEdge]): an edge goes
 * `dependent --depends-on--> prerequisite`, so "following dependencies" walks from a task to its
 * prerequisites.
 */
class DependencyGraph private constructor(
    /** task -> its direct prerequisites. */
    private val prerequisites: Map<TaskId, Set<TaskId>>,
    /** task -> the tasks that directly depend on it. */
    private val dependents: Map<TaskId, Set<TaskId>>,
) {

    /** Tasks that [id] directly depends on (must be completed before [id] can start). */
    fun prerequisitesOf(id: TaskId): Set<TaskId> = prerequisites[id].orEmpty()

    /** Tasks that directly depend on [id] (i.e. tasks that [id] is blocking). */
    fun dependentsOf(id: TaskId): Set<TaskId> = dependents[id].orEmpty()

    /**
     * True if [from] transitively depends on [to]. A node never depends on itself.
     *
     * Iterative DFS so deep chains can't overflow the stack.
     */
    fun dependsOn(from: TaskId, to: TaskId): Boolean {
        if (from == to) return false
        val visited = HashSet<TaskId>()
        val stack = ArrayDeque<TaskId>()
        stack.addAll(prerequisitesOf(from))
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            if (current == to) return true
            if (visited.add(current)) {
                stack.addAll(prerequisitesOf(current))
            }
        }
        return false
    }

    /**
     * Would adding "[dependent] depends on [prerequisite]" create a cycle — either a self-edge, or
     * because [prerequisite] already transitively depends on [dependent]?
     *
     * Use as a guard before persisting a dependency; this is what keeps the stored graph a DAG.
     */
    fun wouldCreateCycle(dependent: TaskId, prerequisite: TaskId): Boolean =
        dependent == prerequisite || dependsOn(from = prerequisite, to = dependent)

    /**
     * True if the graph already contains a cycle. Defensive integrity check when loading edges
     * from storage; the in-app guards should never let this happen.
     *
     * Three-colour DFS: a GRAY node is on the current path, so meeting one again is a back-edge.
     */
    fun hasCycle(): Boolean {
        val color = HashMap<TaskId, Color>()
        val nodes = prerequisites.keys + dependents.keys
        for (node in nodes) {
            if (color[node] == null && dfsHasCycle(node, color)) return true
        }
        return false
    }

    private enum class Color { GRAY, BLACK }

    private fun dfsHasCycle(start: TaskId, color: MutableMap<TaskId, Color>): Boolean {
        val frames = ArrayDeque<Frame>()
        color[start] = Color.GRAY
        frames.addLast(Frame(start, prerequisitesOf(start).iterator()))
        while (frames.isNotEmpty()) {
            val frame = frames.last()
            if (frame.children.hasNext()) {
                val next = frame.children.next()
                when (color[next]) {
                    Color.GRAY -> return true // back-edge: cycle
                    Color.BLACK -> Unit
                    null -> {
                        color[next] = Color.GRAY
                        frames.addLast(Frame(next, prerequisitesOf(next).iterator()))
                    }
                }
            } else {
                color[frame.node] = Color.BLACK
                frames.removeLast()
            }
        }
        return false
    }

    private class Frame(val node: TaskId, val children: Iterator<TaskId>)

    companion object {
        /** Builds a graph from a flat edge list, indexing both directions for O(1) lookups. */
        fun of(edges: Iterable<DependencyEdge>): DependencyGraph {
            val prerequisites = HashMap<TaskId, MutableSet<TaskId>>()
            val dependents = HashMap<TaskId, MutableSet<TaskId>>()
            for (edge in edges) {
                prerequisites.getOrPut(edge.dependentId) { LinkedHashSet() }.add(edge.prerequisiteId)
                dependents.getOrPut(edge.prerequisiteId) { LinkedHashSet() }.add(edge.dependentId)
            }
            return DependencyGraph(prerequisites, dependents)
        }
    }
}
