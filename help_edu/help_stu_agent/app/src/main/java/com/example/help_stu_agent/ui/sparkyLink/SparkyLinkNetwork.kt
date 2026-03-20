package com.example.help_stu_agent.ui.sparkyLink

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object SparkyLinkNetwork {

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS) // AI 生成任务耗时较长，增加读取超时时间
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val BASE_URL = "http://10.29.142.138:8001/"

    val api: SparkyLinkApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SparkyLinkApi::class.java)
    }
}