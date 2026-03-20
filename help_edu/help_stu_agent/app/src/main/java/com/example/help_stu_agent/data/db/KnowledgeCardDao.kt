package com.example.help_stu_agent.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KnowledgeCardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: KnowledgeCardEntity)

    @Query("SELECT * FROM knowledge_cards ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<KnowledgeCardEntity>>

    @Query("SELECT * FROM knowledge_cards ORDER BY createdAt DESC")
    suspend fun getAll(): List<KnowledgeCardEntity>

    @Query("SELECT DISTINCT strftime('%Y-%m-%d', createdAt / 1000, 'unixepoch') FROM knowledge_cards ORDER BY createdAt DESC")
    fun getReportDates(): Flow<List<String>>

    @Query("SELECT * FROM knowledge_cards WHERE strftime('%Y-%m-%d', createdAt / 1000, 'unixepoch') = :date")
    suspend fun getCardsByDate(date: String): List<KnowledgeCardEntity>

    @Query("SELECT * FROM knowledge_cards WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): KnowledgeCardEntity?

    @Query("DELETE FROM knowledge_cards WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM knowledge_cards")
    suspend fun clearAll()
}
