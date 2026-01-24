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

    @Query("SELECT * FROM knowledge_cards WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): KnowledgeCardEntity?

    @Query("DELETE FROM knowledge_cards WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM knowledge_cards")
    suspend fun clearAll()
}
