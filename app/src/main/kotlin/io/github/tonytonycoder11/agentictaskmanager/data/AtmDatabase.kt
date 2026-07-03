package io.github.tonytonycoder11.agentictaskmanager.data

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.tonytonycoder11.agentictaskmanager.data.dao.DependencyDao
import io.github.tonytonycoder11.agentictaskmanager.data.dao.TaskDao
import io.github.tonytonycoder11.agentictaskmanager.data.entity.DependencyEntity
import io.github.tonytonycoder11.agentictaskmanager.data.entity.TaskEntity

/** Room database persisting tasks and their dependency edges. */
@Database(
    entities = [TaskEntity::class, DependencyEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AtmDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun dependencyDao(): DependencyDao

    companion object {
        const val NAME = "agentic_task_manager.db"
    }
}
