package io.github.tonytonycoder11.agentictaskmanager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.tonytonycoder11.agentictaskmanager.data.entity.DependencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DependencyDao {

    @Query("SELECT * FROM dependencies")
    fun observeAll(): Flow<List<DependencyEntity>>

    @Query("SELECT * FROM dependencies")
    suspend fun getAll(): List<DependencyEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(edge: DependencyEntity)

    @Query("DELETE FROM dependencies WHERE dependentId = :dependentId AND prerequisiteId = :prerequisiteId")
    suspend fun delete(dependentId: String, prerequisiteId: String)
}
