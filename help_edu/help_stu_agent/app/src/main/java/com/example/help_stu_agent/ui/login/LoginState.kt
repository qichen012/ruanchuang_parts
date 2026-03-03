package com.example.help_stu_agent.ui.login

sealed class LoginState {
    object Idle : LoginState()                  // 初始状态
    object Loading : LoginState()               // 正在请求网络
    data class Success(val token: String) : LoginState() // 登录成功，拿到 Token
    data class Error(val message: String) : LoginState() // 登录失败
}