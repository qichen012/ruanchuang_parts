package com.example.help_stu_agent.ui.sparkyLink

import java.time.LocalDate

data class SparkLinkBriefRequest(
    val date_a: String,
    val date_b: String,
    val user_id: String,
    val force_regen: Boolean = true,
    val save_to_file: Boolean =true,
    val mock: Boolean = false
)

data class SparkLinkBriefResponse(
    val posterior_insight: String = "",
    val key_concepts: String = ""
)

data class CalendarCell(
    val date: LocalDate?,
    val isCurrentMonth: Boolean
)

data class ReportPreviewItem(
    val title: String
)
