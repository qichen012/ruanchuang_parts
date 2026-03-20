package com.example.help_stu_agent.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_logs")
data class PhotoLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Int,
    val fileName: String,
    val localUri: String,
    val createdAt: Long = System.currentTimeMillis()
)
