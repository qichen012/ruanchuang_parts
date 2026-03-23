package com.example.help_stu_agent.data.repo

import com.example.help_stu_agent.ui.sparkyLink.SparkLinkBriefRequest
import com.example.help_stu_agent.ui.sparkyLink.SparkLinkBriefResponse
import com.example.help_stu_agent.ui.sparkyLink.SparkyLinkApi
import com.example.help_stu_agent.ui.sparkyLink.SparkyLinkNetwork

class SparkyLinkRepository(
    private val api: SparkyLinkApi = SparkyLinkNetwork.api
) {
    suspend fun generateBrief(request: SparkLinkBriefRequest): SparkLinkBriefResponse {
        return api.generateSparkLinkBrief(request)
    }
}