package io.github.tonytonycoder11.agentictaskmanager.domain.model

/**
 * How urgent a task is. Ordered from least to most important, so the natural enum
 * ordinal can be used directly for "highest priority first" sorting.
 */
enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT,
}
