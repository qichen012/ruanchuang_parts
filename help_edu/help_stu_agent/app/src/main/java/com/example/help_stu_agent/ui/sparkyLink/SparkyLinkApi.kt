package com.example.help_stu_agent.ui.sparkyLink

import retrofit2.http.Body
import retrofit2.http.POST

interface SparkyLinkApi {

    @POST("/generate_sparklink_brief")
    suspend fun generateSparkLinkBrief(
        @Body request: SparkLinkBriefRequest
    ): SparkLinkBriefResponse
}