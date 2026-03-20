package com.example.help_stu_agent.ui.openSource

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenSourceApiService {
    @GET("opensource/projects")
    suspend fun getProjects(
        @Query("keyword") keyword: String,
        @Query("language") language: String = "python",
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 10
    ): OpenSourceResponse
}