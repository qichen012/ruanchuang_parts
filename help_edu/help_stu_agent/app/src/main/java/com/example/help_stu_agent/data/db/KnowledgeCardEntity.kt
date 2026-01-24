package com.example.help_stu_agent.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "knowledge_cards",
    indices = [
        Index(value = ["createdAt"]),
        Index(value = ["category"])
    ]
)
data class KnowledgeCardEntity(
    @PrimaryKey val id: String,

    val createdAt: Long,

    // 可选：用于你后续“从历史定位到原始 PDF”
    val pdfDisplayName: String?,
    val pdfUri: String?,

    // 预览字段（首页卡片直接用，不必每次都解析 rawJson）
    val category: String?,
    val colorHex: String?,      // meta.color，例如 "#FF5733"
    val headerTitle: String?,   // header.title
    val headerSubtitle: String?,// header.subtitle
    val footerQuote: String?,   // footer.quote

    // 后端原始返回 JSON（详情页解析 key_points/summary）
    val rawJson: String
)
