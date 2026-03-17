package com.example.help_stu_agent.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EliteIdeaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ideas: List<EliteIdeaEntity>)

    @Query("SELECT * FROM elite_ideas ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<EliteIdeaEntity>>

    @Query("DELETE FROM elite_ideas")
    suspend fun clearAll()
}