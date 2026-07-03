package io.github.tonytonycoder11.agentictaskmanager.domain.model

/**
 * Task urgency, ordered least to most important so the enum ordinal drives "highest first" sorting.
 */
enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT,
}
