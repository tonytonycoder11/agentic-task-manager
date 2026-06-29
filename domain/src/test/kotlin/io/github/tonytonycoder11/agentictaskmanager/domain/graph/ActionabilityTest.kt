package io.github.tonytonycoder11.agentictaskmanager.domain.graph

import io.github.tonytonycoder11.agentictaskmanager.domain.fake.task
import io.github.tonytonycoder11.agentictaskmanager.domain.model.DependencyEdge
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskId
import io.github.tonytonycoder11.agentictaskmanager.domain.model.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests the central actionability rule via the shared [TaskInsights] projection. */
class ActionabilityTest {

    private fun edge(dependent: String, prerequisite: String) =
        DependencyEdge(TaskId(dependent), TaskId(prerequisite))

    @Test
    fun `an open task with no prerequisites is actionable`() {
        val insights = TaskInsights.computeAll(listOf(task("A")), emptyList())

        assertTrue(insights.single().isActionable)
        assertTrue(insights.single().blockedBy.isEmpty())
    }

    @Test
    fun `an open task with an open prerequisite is blocked`() {
        val tasks = listOf(task("A"), task("B"))
        val insights = TaskInsights.computeAll(tasks, listOf(edge("B", "A")))

        val b = insights.first { it.task.id == TaskId("B") }
        assertFalse(b.isActionable)
        assertEquals(listOf(TaskId("A")), b.blockedBy)
    }

    @Test
    fun `an open task becomes actionable once every prerequisite is completed`() {
        val tasks = listOf(task("A", status = TaskStatus.COMPLETED), task("B"))
        val insights = TaskInsights.computeAll(tasks, listOf(edge("B", "A")))

        val b = insights.first { it.task.id == TaskId("B") }
        assertTrue(b.isActionable)
        assertTrue(b.blockedBy.isEmpty())
    }

    @Test
    fun `a completed task is never actionable`() {
        val insights = TaskInsights.computeAll(listOf(task("A", status = TaskStatus.COMPLETED)), emptyList())

        assertFalse(insights.single().isActionable)
    }

    @Test
    fun `blocks lists the dependents a task is holding up`() {
        val tasks = listOf(task("A"), task("B"), task("C"))
        val insights = TaskInsights.computeAll(tasks, listOf(edge("B", "A"), edge("C", "A")))

        val a = insights.first { it.task.id == TaskId("A") }
        assertEquals(setOf(TaskId("B"), TaskId("C")), a.blocks.toSet())
    }
}
