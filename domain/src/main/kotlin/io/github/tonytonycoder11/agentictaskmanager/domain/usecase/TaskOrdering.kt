package io.github.tonytonycoder11.agentictaskmanager.domain.usecase

import io.github.tonytonycoder11.agentictaskmanager.domain.graph.TaskInsight
import java.time.Instant

/**
 * Shared ordering for task lists returned by the query use cases:
 * highest priority first, then the earliest due date, then title (so the order is stable and
 * deterministic). Tasks without a due date sort after those that have one.
 */
internal val byUrgency: Comparator<TaskInsight> =
    compareByDescending<TaskInsight> { it.task.priority.ordinal }
        .thenBy { it.task.dueAt ?: Instant.MAX }
        .thenBy { it.task.title.lowercase() }
