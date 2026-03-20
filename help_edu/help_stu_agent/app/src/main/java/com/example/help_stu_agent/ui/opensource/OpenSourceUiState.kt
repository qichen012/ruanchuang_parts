package com.example.help_stu_agent.ui.openSource

data class OpenSourceUiState(
    val isLoading: Boolean = false,
    val projects: List<GitHubProjectInfo> = emptyList(),
    val error: String? = null,
    val query: String = "ai"
)