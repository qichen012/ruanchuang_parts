package com.example.help_stu_agent.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EliteIdeaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<EliteIdeaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: EliteIdeaEntity)

    @Query("SELECT * FROM elite_ideas ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<EliteIdeaEntity>>

    @Query("SELECT * FROM elite_ideas ORDER BY createdAt DESC")
    suspend fun getAll(): List<EliteIdeaEntity>

    @Query("SELECT * FROM elite_ideas WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EliteIdeaEntity?

    @Query("""
        SELECT * FROM elite_ideas 
        WHERE createdAt >= :startMillis AND createdAt < :endMillis 
        ORDER BY createdAt DESC
    """)
    fun observeByDate(startMillis: Long, endMillis: Long): Flow<List<EliteIdeaEntity>>

    @Query("SELECT DISTINCT strftime('%Y-%m-%d', createdAt / 1000, 'unixepoch') FROM elite_ideas ORDER BY createdAt DESC")
    fun getReportDates(): Flow<List<String>>

    @Query("DELETE FROM elite_ideas")
    suspend fun clearAll()

    @Query("DELETE FROM elite_ideas WHERE id = :id")
    suspend fun deleteById(id: String)
}