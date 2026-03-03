package com.example.help_stu_agent.data.net

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// 1. 定义请求和响应的数据类
data class AuthRequest(val email: String, val password: String)
data class AuthResponse(val token: String?, val message: String?, val code: Int?)

// 2. 定义 API 接口
interface ApiService {
    @POST("/api/login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    @POST("/api/register")
    suspend fun register(@Body request: AuthRequest): Response<AuthResponse>
}

// 3. 构建 Retrofit 实例
object RetrofitClient {
    // 注意：如果你在 Android 模拟器上测试本地的 FastAPI 后端，请使用 10.0.2.2 替代 localhost
    private const val BASE_URL = "http://10.0.2.2:8000"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}