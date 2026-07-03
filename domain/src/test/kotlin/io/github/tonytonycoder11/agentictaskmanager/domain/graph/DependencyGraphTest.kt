package io.github.tonytonycoder11.agentictaskmanager.domain.graph

import io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cycle-detection / reachability core. Edge convention: `dependent -> prerequisite`. */
class DependencyGraphTest {

    private fun id(v: String) = TaskId(v)
    private fun edge(dependent: String, prerequisite: String) =
        DependencyEdge(id(dependent), id(prerequisite))

    @Test
    fun `indexes both directions of an edge`() {
        val graph = DependencyGraph.of(listOf(edge("B", "A")))

        assertEquals(setOf(id("A")), graph.prerequisitesOf(id("B")))
        assertEquals(setOf(id("B")), graph.dependentsOf(id("A")))
        assertEquals(emptySet<TaskId>(), graph.prerequisitesOf(id("A")))
    }

    @Test
    fun `dependsOn follows the chain transitively`() {
        val graph = DependencyGraph.of(listOf(edge("C", "B"), edge("B", "A")))

        assertTrue(graph.dependsOn(from = id("C"), to = id("A")))
        assertTrue(graph.dependsOn(from = id("C"), to = id("B")))
        assertFalse(graph.dependsOn(from = id("A"), to = id("C")))
        assertFalse(graph.dependsOn(from = id("A"), to = id("A")))
    }

    @Test
    fun `wouldCreateCycle rejects a direct back-edge`() {
        // Adding "A depends on B" closes a 2-cycle.
        val graph = DependencyGraph.of(listOf(edge("B", "A")))

        assertTrue(graph.wouldCreateCycle(dependent = id("A"), prerequisite = id("B")))
    }

    @Test
    fun `wouldCreateCycle rejects a transitive back-edge`() {
        // Adding "A depends on C" closes a 3-cycle.
        val graph = DependencyGraph.of(listOf(edge("C", "B"), edge("B", "A")))

        assertTrue(graph.wouldCreateCycle(dependent = id("A"), prerequisite = id("C")))
    }

    @Test
    fun `wouldCreateCycle rejects a self-edge`() {
        val graph = DependencyGraph.of(emptyList())

        assertTrue(graph.wouldCreateCycle(dependent = id("A"), prerequisite = id("A")))
    }

    @Test
    fun `wouldCreateCycle allows an edge that keeps the graph acyclic`() {
        val graph = DependencyGraph.of(listOf(edge("B", "A")))

        assertFalse(graph.wouldCreateCycle(dependent = id("C"), prerequisite = id("A")))
    }

    @Test
    fun `hasCycle is false for a DAG`() {
        val graph = DependencyGraph.of(listOf(edge("C", "B"), edge("B", "A"), edge("C", "A")))

        assertFalse(graph.hasCycle())
    }

    @Test
    fun `hasCycle detects a cycle built from raw edges`() {
        // Two opposing edges form a cycle; hasCycle is the integrity check for data loaded from storage.
        val graph = DependencyGraph.of(listOf(edge("A", "B"), edge("B", "A")))

        assertTrue(graph.hasCycle())
    }
}
