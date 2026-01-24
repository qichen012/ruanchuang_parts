package com.example.help_stu_agent.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "knowledge_tree")
data class KnowledgeTreeEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val pdfDisplayName: String?,
    val pdfUri: String?,      // 可选：content://...
    val jsonPath: String,
    val nodeCount: Int? = null,
    val summary: String? = null
)
