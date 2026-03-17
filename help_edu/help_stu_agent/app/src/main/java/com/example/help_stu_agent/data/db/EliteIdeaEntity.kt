package com.example.help_stu_agent.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "elite_ideas")
data class EliteIdeaEntity(
    @PrimaryKey val id: String,
    val category: String,
    val title: String,
    val description: String,
    val instancesJson: String,
    val createdAt: Long
)