package io.github.tonytonycoder11.agentictaskmanager.domain.support

import java.util.UUID

/**
 * Produces new unique task ids.
 *
 * Abstracted behind an interface purely for testability: production uses [UuidIdGenerator],
 * while unit tests inject a deterministic counter so generated ids are predictable. (UUID
 * itself is plain JVM, so even the default implementation stays Android-free.)
 */
fun interface IdGenerator {
    fun newId(): String
}

/** Default implementation: a random UUID string. */
class UuidIdGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
