package com.example.help_stu_agent.data.net

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

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
}

object PdfRetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"

    val api: LearningApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LearningApi::class.java)
    }
}