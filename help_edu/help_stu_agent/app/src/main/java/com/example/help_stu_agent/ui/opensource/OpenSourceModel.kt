package com.example.help_stu_agent.ui.openSource

data class GitHubProjectInfo(
    val title: String,
    val author: String,
    val description: String,
    val stars: String,
    val forks: String,
    val tag: String,
    val isAvatar: Boolean,
    val avatarUrl: String,
    val url: String
)

data class OpenSourceData(
    val keyword: String,
    val language: String,
    val pageNum: Int,
    val pageSize: Int,
    val total: Int,
    val projects: List<GitHubProjectInfo>
)

data class OpenSourceResponse(
    val code: Int,
    val message: String,
    val timestamp: String,
    val data: OpenSourceData
)