package com.example.help_stu_agent.ui.login

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val token: String, val userId: Int) : LoginState()
    data class Error(val message: String) : LoginState()
}