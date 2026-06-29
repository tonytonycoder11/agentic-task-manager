package io.github.tonytonycoder11.agentictaskmanager.domain.graph

import io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId

/**
 * An immutable view over the task dependency relationships.
 *
 * Built once from a flat list of [DependencyEdge]s, it answers the graph questions the rest
 * of the domain needs:
 *  - who blocks whom ([prerequisitesOf] / [dependentsOf]),
 *  - reachability ([dependsOn]),
 *  - and — the whole reason this graph exists — whether adding a new edge would introduce a
 *    cycle ([wouldCreateCycle]) and whether the graph is currently a DAG ([hasCycle]).
 *
 * Edge direction convention (see [DependencyEdge]): an edge goes
 * `dependent --depends-on--> prerequisite`. So "following dependencies" means walking from a
 * task to its prerequisites.
 *
 * This class is pure and side-effect free; it is the most unit-tested unit in the project.
 */
class DependencyGraph private constructor(
    /** task -> the set of tasks it directly depends on (its prerequisites). */
    private val prerequisites: Map<TaskId, Set<TaskId>>,
    /** task -> the set of tasks that directly depend on it (the tasks it blocks). */
    private val dependents: Map<TaskId, Set<TaskId>>,
) {

    /** Tasks that [id] directly depends on (must be completed before [id] can start). */
    fun prerequisitesOf(id: TaskId): Set<TaskId> = prerequisites[id].orEmpty()

    /** Tasks that directly depend on [id] (i.e. tasks that [id] is blocking). */
    fun dependentsOf(id: TaskId): Set<TaskId> = dependents[id].orEmpty()

    /**
     * True if [from] transitively depends on [to] — i.e. there is a directed path
     * `from --depends-on--> ... --depends-on--> to`. A node never "depends on" itself here
     * (an empty path is not a dependency).
     *
     * Implemented as an iterative depth-first search over [prerequisitesOf] so it is safe for
     * deep chains without risking a stack overflow.
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
     * Would adding the edge "[dependent] depends on [prerequisite]" create a cycle?
     *
     * Two ways that can happen:
     *  1. a self-edge ([dependent] == [prerequisite]); or
     *  2. [prerequisite] already (transitively) depends on [dependent], so the new edge would
     *     close the loop `dependent -> prerequisite -> ... -> dependent`.
     *
     * Callers use this as a guard BEFORE persisting a new dependency, which is what keeps the
     * stored graph a DAG at all times.
     */
    fun wouldCreateCycle(dependent: TaskId, prerequisite: TaskId): Boolean =
        dependent == prerequisite || dependsOn(from = prerequisite, to = dependent)

    /**
     * True if the current graph already contains a cycle. Used as a defensive integrity check
     * when loading edges from storage (which the in-app guards should never allow to happen).
     *
     * Standard three-colour DFS: a node painted GRAY is on the current recursion path; meeting
     * a GRAY node again means a back-edge, i.e. a cycle.
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
        // Iterative DFS that mirrors the recursive white/gray/black algorithm.
        // Each stack frame remembers which prerequisites are still to be explored.
        val frames = ArrayDeque<Frame>()
        color[start] = Color.GRAY
        frames.addLast(Frame(start, prerequisitesOf(start).iterator()))
        while (frames.isNotEmpty()) {
            val frame = frames.last()
            if (frame.children.hasNext()) {
                val next = frame.children.next()
                when (color[next]) {
                    Color.GRAY -> return true // back-edge to a node on the current path
                    Color.BLACK -> Unit // already fully explored, skip
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
