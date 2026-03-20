package com.example.help_stu_agent.ui.openSource

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OpenSourceNetwork {

    // 模拟器访问本机后端
    // 如果你是真机，把 10.0.2.2 改成你电脑的局域网 IP，比如 192.168.1.20
    private const val BASE_URL = "http://10.29.238.57:8003/"

    val api: OpenSourceApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenSourceApiService::class.java)
    }
}