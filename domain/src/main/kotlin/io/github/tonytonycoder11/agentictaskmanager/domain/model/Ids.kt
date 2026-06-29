package io.github.tonytonycoder11.agentictaskmanager.domain.model

/**
 * Type-safe identifier for a [Task].
 *
 * Modelled as an inline value class so it costs nothing at runtime (it is just a String)
 * yet the compiler still prevents passing a raw String — or, worse, the wrong id — where a
 * [TaskId] is expected. The whole graph logic traffics in [TaskId], so this small wrapper
 * removes an entire class of "I swapped two ids" bugs.
 */
@JvmInline
value class TaskId(val value: String) {
    init {
        require(value.isNotBlank()) { "TaskId must not be blank" }
    }
}
