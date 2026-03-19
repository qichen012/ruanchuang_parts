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

    // 用户信息
    val userId: Int = 0,

    // 原始数据
    val rawText: String,           // ASR 识别出的原始文本
    val summary: String,           // AI 摘要
    val pointsJson: String,        // points JSON 数组字符串
    val todosJson: String,         // todos JSON 数组字符串

    // 音频文件信息
    val audioFileName: String,     // 音频文件名称
    val audioFileSize: Long,       // 音频文件大小（字节）
    val audioLocalPath: String?,   // 本地缓存路径（可选）

    // 时间戳
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // 可选的元数据
    val courseName: String = "",   // 课程名称
    val topic: String = "",        // 主题
    val duration: Long = 0L        // 录音时长（秒）
)
