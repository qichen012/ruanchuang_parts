package com.example.help_stu_agent.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.help_stu_agent.data.net.AuthRequest
import com.example.help_stu_agent.data.net.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    // 内部可变的 StateFlow
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    // 暴露给 UI 的只读 StateFlow
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun login(email: String, password: String) {
        // 简单的数据校验
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("邮箱或密码不能为空")
            return
        }

        // 开启协程进行网络请求
        viewModelScope.launch {
            _loginState.value = LoginState.Loading // 通知 UI 显示加载圈

            try {
                val response = RetrofitClient.apiService.login(AuthRequest(email, password))
                val body = response.body()

                // 检查是否成功并且 token 和 user_id 都不为空
                if (response.isSuccessful && body?.token != null && body?.user_id != null) {
                    val token = body.token
                    val userId = body.user_id // 提取后端返回的 user_id

                    // 将两者一起传入 Success 状态
                    _loginState.value = LoginState.Success(token, userId)
                } else {
                    val errorMsg = body?.message ?: "登录失败，请检查账号密码"
                    _loginState.value = LoginState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("网络连接异常: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}