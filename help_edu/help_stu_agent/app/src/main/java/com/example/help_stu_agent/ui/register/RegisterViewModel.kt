package com.example.help_stu_agent.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.help_stu_agent.data.net.AuthRequest
import com.example.help_stu_agent.data.net.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {
    private val _registerState = MutableStateFlow<LoginState>(LoginState.Idle) // 复用 LoginState 即可
    val registerState: StateFlow<LoginState> = _registerState.asStateFlow()

    fun register(email: String, password: String, confirmPwd: String) {
        if (email.isBlank() || password.isBlank()) {
            _registerState.value = LoginState.Error("邮箱或密码不能为空")
            return
        }
        if (password != confirmPwd) {
            _registerState.value = LoginState.Error("两次输入的密码不一致")
            return
        }

        viewModelScope.launch {
            _registerState.value = LoginState.Loading
            try {
                // 调用网络请求里的 register 接口
                val response = RetrofitClient.apiService.register(AuthRequest(email, password))
                if (response.isSuccessful && response.body()?.token != null) {
                    _registerState.value = LoginState.Success(response.body()!!.token!!)
                } else {
                    val errorMsg = response.body()?.message ?: "注册失败，邮箱可能已存在"
                    _registerState.value = LoginState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _registerState.value = LoginState.Error("网络连接异常: ${e.localizedMessage}")
            }
        }
    }

    fun resetState() {
        _registerState.value = LoginState.Idle
    }
}