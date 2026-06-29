package io.github.tonytonycoder11.agentictaskmanager.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room row for a task.
 *
 * Enums and the due instant are stored as primitives (String / nullable Long epoch millis) to
 * keep the schema simple and avoid TypeConverters in Phase 1. Conversion to/from the domain
 * [Task][io.github.tonytonycoder11.agentictaskmanager.domain.model.Task] lives in the mappers.
 *
 * [parentId] is indexed (sub-task lookups walk it) but intentionally has no foreign key: the
 * sub-task subtree deletion is owned by the domain use case, not by a DB cascade.
 */
@Entity(
    tableName = "tasks",
    indices = [Index("parentId")],
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val priority: String,
    val dueAtEpochMillis: Long?,
    val status: String,
    val recurrence: String,
    val parentId: String?,
)
