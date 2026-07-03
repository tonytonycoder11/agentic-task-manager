package io.github.tonytonycoder11.agentictaskmanager.domain.support

import java.util.UUID

/**
 * Produces new unique task ids.
 *
 * An interface for testability: tests inject a deterministic counter, production uses [UuidIdGenerator].
 */
fun interface IdGenerator {
    fun newId(): String
}

/** Default implementation: a random UUID string. */
class UuidIdGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
