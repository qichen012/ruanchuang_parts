package com.example.help_stu_agent.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "meeting_minutes",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["userId"])
    ]
)
data class MeetingMinutesEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val userId: Int = 0,

    // 原始内容
    val rawText: String,
    val summary: String,
    val pointsJson: String,
    val todosJson: String,

    // 新接口返回的 markdown 全文
    val minutesMarkdown: String = "",

    // 音频信息
    val audioFileName: String,
    val audioFileSize: Long,
    val audioLocalPath: String?,

    // 时间
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // 额外元数据
    val courseName: String = "",
    val topic: String = "",
    val duration: Long = 0L
)