package io.github.tonytonycoder11.agentictaskmanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Room row for one dependency edge: [dependentId] depends on [prerequisiteId].
 *
 * Both columns are foreign keys onto `tasks.id` with ON DELETE CASCADE, so deleting a task removes
 * every edge referencing it and the persisted graph can never hold a dangling edge.
 */
@Entity(
    tableName = "dependencies",
    primaryKeys = ["dependentId", "prerequisiteId"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["dependentId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["prerequisiteId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("dependentId"), Index("prerequisiteId")],
)
data class DependencyEntity(
    val dependentId: String,
    val prerequisiteId: String,
)
