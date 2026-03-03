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
                if (response.isSuccessful && response.body()?.token != null) {
                    // 登录成功
                    _loginState.value = LoginState.Success(response.body()!!.token!!)
                } else {
                    // 登录失败 (密码错误等)
                    val errorMsg = response.body()?.message ?: "登录失败，请检查账号密码"
                    _loginState.value = LoginState.Error(errorMsg)
                }
            } catch (e: Exception) {
                // 网络异常或服务器崩溃
                _loginState.value = LoginState.Error("网络连接异常: ${e.localizedMessage}")
            }
        }
    }

    // 重置状态 (例如在显示完错误提示后)
    fun resetState() {
        _loginState.value = LoginState.Idle
    }
}