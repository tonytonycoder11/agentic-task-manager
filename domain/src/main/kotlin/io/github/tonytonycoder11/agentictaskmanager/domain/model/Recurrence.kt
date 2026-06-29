package io.github.tonytonycoder11.agentictaskmanager.domain.model

/**
 * How (and whether) a task repeats once it is completed.
 *
 * When a recurring task is completed, the use case spawns the *next* occurrence as a fresh
 * OPEN task with its due date advanced by the period below. This keeps recurrence as real
 * domain behaviour rather than a cosmetic flag. Calendar math for [MONTHLY] is done in UTC
 * for determinism (documented simplification for Phase 1).
 */
enum class Recurrence {
    /** Does not repeat. */
    NONE,

    /** Repeats every day. */
    DAILY,

    /** Repeats every 7 days (covers the "every Monday" example from the brief). */
    WEEKLY,

    /** Repeats on the same day each month. */
    MONTHLY,
}
