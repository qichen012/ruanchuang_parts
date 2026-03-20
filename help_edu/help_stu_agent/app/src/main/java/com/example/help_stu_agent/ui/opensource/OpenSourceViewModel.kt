package com.example.help_stu_agent.ui.openSource

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OpenSourceViewModel(
    private val repository: OpenSourceRepository = OpenSourceRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpenSourceUiState())
    val uiState: StateFlow<OpenSourceUiState> = _uiState.asStateFlow()

    init {
        loadProjects("ai")
    }

    fun loadProjects(keyword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                query = keyword
            )

            try {
                val projects = repository.getProjects(
                    keyword = keyword,
                    language = "",
                    pageNum = 1,
                    pageSize = 10
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    projects = projects
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            }
        }
    }
}