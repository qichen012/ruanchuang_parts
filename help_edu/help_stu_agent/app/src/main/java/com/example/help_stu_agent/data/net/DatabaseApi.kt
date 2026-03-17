package com.example.help_stu_agent.data.net

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class UserResponse(
    val id: Int,
    val email: String?,
    val name: String?,
    val gender: String?,
    val age: Int?
)

data class SourceDocumentCreateRequest(
    val user_id: Int,
    val file_name: String,
    val file_path: String,
    val upload_date: String,
    val processed_status: String
)

data class SourceDocumentUpdateRequest(
    val processed_status: String
)

data class SourceDocumentResponse(
    val id: Int,
    val user_id: Int,
    val file_name: String,
    val file_path: String,
    val upload_date: String,
    val processed_status: String
)

data class AppUsageCreate(
    val user_id: Int,
    val start_time: String, // 格式: "2023-10-27T10:00:00" (ISO 8601)
    val end_time: String,   // 格式: "2023-10-27T10:30:00"
    val duration_seconds: Int
)

data class AppUsageStatsResponse(
    val data_points: List<Float>
)

interface LearningApi {
    @POST("api/v1/source-documents")
    suspend fun createSourceDocument(
        @Body request: SourceDocumentCreateRequest
    ): SourceDocumentResponse

    @PUT("api/v1/source-documents/{doc_id}")
    suspend fun updateSourceDocumentStatus(
        @Path("doc_id") docId: Int,
        @Body request: SourceDocumentUpdateRequest
    ): SourceDocumentResponse

    @POST("api/v1/app-usage")
    suspend fun recordAppUsage(@Body usage: AppUsageCreate)

    @GET("api/v1/app-usage/stats/{user_id}")
    suspend fun getUsageStats(@Path("user_id") userId: Int): AppUsageStatsResponse

    @GET("api/v1/users/{user_id}")
    suspend fun getUserInfo(@Path("user_id") userId: Int): UserResponse
}


object PdfRetrofitClient {
    private const val BASE_URL = "http://10.29.238.57:8000/"

    val api: LearningApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LearningApi::class.java)
    }
}