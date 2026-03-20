package com.example.help_stu_agent.ui.sparkyLink

class SparkyLinkRepository(
    private val api: SparkyLinkApi = SparkyLinkNetwork.api
) {
    suspend fun generateBrief(request: SparkLinkBriefRequest): SparkLinkBriefResponse {
        return api.generateSparkLinkBrief(request)
    }
}