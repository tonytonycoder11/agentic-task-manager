package io.github.tonytonycoder11.agentictaskmanager.domain.model

/**
 * Type-safe identifier for a [Task]. Inline value class: zero runtime cost, but the compiler
 * rejects passing a raw String or the wrong id where a [TaskId] is expected.
 */
@JvmInline
value class TaskId(val value: String) {
    init {
        require(value.isNotBlank()) { "TaskId must not be blank" }
    }
}
