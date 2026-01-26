package com.example.help_stu_agent

import android.net.Uri

object AppRoutes {
    const val Main = "main"
    const val Upload = "upload"
    const val KnowledgeTree = "knowledgeTree"
    const val KnowledgeTreeHistory = "knowledge_tree_history"
    const val SparkyLink = "sparky_link"
    const val KnowledgeTreeById = "knowledgeTree/{treeId}"
    fun knowledgeTreeById(treeId: String): String =
        "knowledgeTree/${Uri.encode(treeId)}"
    const val DailyReport = "daily_report"
    const val KnowledgeCardDetail = "knowledge_card_detail/{cardId}"
    fun knowledgeCardDetail(cardId: String): String =
        "knowledge_card_detail/${Uri.encode(cardId)}"

    const val EliteIdeas = "elite_ideas"

}
