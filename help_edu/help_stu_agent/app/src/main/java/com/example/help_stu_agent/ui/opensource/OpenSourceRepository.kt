package com.example.help_stu_agent.ui.openSource

class OpenSourceRepository(
    private val api: OpenSourceApiService = OpenSourceNetwork.api
) {
    suspend fun getProjects(
        keyword: String,
        language: String = "python",
        pageNum: Int = 1,
        pageSize: Int = 10
    ): List<GitHubProjectInfo> {
        val response = api.getProjects(
            keyword = keyword,
            language = language,
            pageNum = pageNum,
            pageSize = pageSize
        )

        if (response.code != 200) {
            throw RuntimeException(response.message.ifBlank { "后端返回失败" })
        }

        return response.data.projects
    }
}