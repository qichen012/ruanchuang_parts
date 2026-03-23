package com.example.help_stu_agent.ui.sparkyLink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.help_stu_agent.data.repo.SparkyLinkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private fun todayString(): String {
    return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
}

data class SparkyLinkUiState(
    val dateA: String = todayString(),
    val dateB: String = todayString(),
    val userId: String = "test_user",
    val forceRegen: Boolean = false,
    val saveToFile: Boolean = true,
    val mock: Boolean = false,

    val isLoading: Boolean = false,
    val posteriorInsight: String = "",
    val keyConcepts: String = "",
    val error: String? = null
)

class SparkyLinkViewModel(
    private val repository: SparkyLinkRepository = SparkyLinkRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SparkyLinkUiState())
    val uiState: StateFlow<SparkyLinkUiState> = _uiState.asStateFlow()

    fun updateDateA(value: String) {
        _uiState.value = _uiState.value.copy(dateA = value)
    }

    fun updateDateB(value: String) {
        _uiState.value = _uiState.value.copy(dateB = value)
    }

    fun updateUserId(value: String) {
        _uiState.value = _uiState.value.copy(userId = value)
    }

    fun updateForceRegen(value: Boolean) {
        _uiState.value = _uiState.value.copy(forceRegen = value)
    }

    fun updateSaveToFile(value: Boolean) {
        _uiState.value = _uiState.value.copy(saveToFile = value)
    }

    fun updateMock(value: Boolean) {
        _uiState.value = _uiState.value.copy(mock = value)
    }

    fun generateSparkLinkBrief() {
        val current = _uiState.value

        _uiState.value = current.copy(
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            runCatching {
                repository.generateBrief(
                    SparkLinkBriefRequest(
                        date_a = current.dateA,
                        date_b = current.dateB,
                        user_id = current.userId,
                        force_regen = current.forceRegen,
                        save_to_file = current.saveToFile,
                        mock = current.mock
                    )
                )
            }.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    posteriorInsight = response.posterior_insight,
                    keyConcepts = response.key_concepts,
                    error = null
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "请求失败"
                )
            }
        }
    }
}