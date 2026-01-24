package com.example.help_stu_agent.data.db

import androidx.room.*

@Dao
interface KnowledgeTreeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: KnowledgeTreeEntity)

    @Query("SELECT * FROM knowledge_tree ORDER BY createdAt DESC")
    suspend fun listAll(): List<KnowledgeTreeEntity>

    @Query("SELECT * FROM knowledge_tree WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): KnowledgeTreeEntity?

    @Query("DELETE FROM knowledge_tree WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM knowledge_tree")
    suspend fun clearAll()
}
