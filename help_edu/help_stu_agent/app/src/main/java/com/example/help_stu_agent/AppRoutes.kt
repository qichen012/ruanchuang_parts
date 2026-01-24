package com.example.help_stu_agent

import android.net.Uri

object AppRoutes {
    const val Main = "main"
    const val Upload = "upload"
    const val KnowledgeTree = "knowledgeTree"
    const val KnowledgeTreeHistory = "knowledge_tree_history"

    const val KnowledgeCardDetail = "knowledge_card_detail/{cardId}"
    fun knowledgeCardDetail(cardId: String): String =
        "knowledge_card_detail/${Uri.encode(cardId)}"
}
