package com.example.help_stu_agent.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.help_stu_agent.data.net.RegisterRequest
import com.example.help_stu_agent.data.net.RetrofitClient
import com.example.help_stu_agent.ui.login.LoginState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {
    private val _registerState = MutableStateFlow<LoginState>(LoginState.Idle) // 复用 LoginState
    val registerState: StateFlow<LoginState> = _registerState.asStateFlow()

    fun register(
        email: String,
        password: String,
        confirmPwd: String,
        name: String,
        gender: String,
        age: String // 界面输入为 String，转换逻辑放在这里
    ) {
        // 基础校验
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            _registerState.value = LoginState.Error("请填写完整信息")
            return
        }
        if (password != confirmPwd) {
            _registerState.value = LoginState.Error("两次输入的密码不一致")
            return
        }

        val ageInt = age.toIntOrNull() ?: 0

        viewModelScope.launch {
            _registerState.value = LoginState.Loading
            try {
                val request = RegisterRequest(email, password, name, gender, ageInt)
                val response = RetrofitClient.apiService.register(request)

                val body = response.body()
                if (response.isSuccessful && body?.token != null && body?.user_id != null) {
                    // 注册成功，返回 token 和 userId
                    _registerState.value = LoginState.Success(body.token, body.user_id)
                } else {
                    _registerState.value = LoginState.Error(body?.message ?: "注册失败")
                }
            } catch (e: Exception) {
                _registerState.value = LoginState.Error("网络连接异常")
            }
        }
    }

    fun resetState() {
        _registerState.value = LoginState.Idle
    }
}