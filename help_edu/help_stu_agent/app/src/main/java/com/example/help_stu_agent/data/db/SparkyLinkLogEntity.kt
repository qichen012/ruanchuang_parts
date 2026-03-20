package com.example.help_stu_agent.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sparky_link_logs")
data class SparkyLinkLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Int,
    val dateA: String,
    val dateB: String,
    val insight: String,
    val concepts: String,
    val createdAt: Long = System.currentTimeMillis()
)
