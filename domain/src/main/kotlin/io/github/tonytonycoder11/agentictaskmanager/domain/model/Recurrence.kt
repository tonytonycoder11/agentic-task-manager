package io.github.tonytonycoder11.agentictaskmanager.domain.model

/**
 * How (and whether) a task repeats once completed. On completion the next occurrence is spawned
 * as a fresh OPEN task with its due date advanced by the period. [MONTHLY] math runs in UTC for
 * determinism.
 */
enum class Recurrence {
    NONE,
    DAILY,

    /** Repeats every 7 days. */
    WEEKLY,

    /** Repeats on the same day each month. */
    MONTHLY,
}
