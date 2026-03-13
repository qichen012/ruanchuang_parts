package com.example.help_stu_agent.ui.home // 根据你的实际包名调整

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.help_stu_agent.data.net.AppUsageCreate
import com.example.help_stu_agent.data.net.PdfRetrofitClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserViewModel : ViewModel() {

    var peakTimeLabel by mutableStateOf("--:--")
        private set

    var userName by mutableStateOf("Loading...")
        private set

    var userEmail by mutableStateOf("Loading...")
        private set

    var userAge by mutableStateOf(0)
        private set

    var userGender by mutableStateOf("male")
        private set

    private val api = PdfRetrofitClient.api

    var usageDataPoints by mutableStateOf(List(24) { 0f })
        private set

    private fun calculatePeakTime(data: List<Float>) {
        if (data.all { it == 0f }) {
            peakTimeLabel = "--:--"
            return
        }

        val maxIndex = data.indexOf(data.maxOrNull() ?: 0f)
        // 格式化显示：例如 index 为 11，显示 "11:00 - 12:00"
        val startHour = maxIndex
        val endHour = maxIndex + 1

        val startTime = String.format("%02d:00", startHour)
        val endTime = String.format("%02d:00", endHour)

        peakTimeLabel = "$startTime - $endTime"
    }
    fun fetchUserInfo(userId: Int) {
        viewModelScope.launch {
            try {
                val response = api.getUserInfo(userId)

                userName = if (!response.name.isNullOrEmpty()) response.name else "Agent User"
                userEmail = response.email ?: "No Email Provided"

                userAge = response.age ?: 0
                userGender = response.gender ?: "male"

            } catch (e: Exception) {
                e.printStackTrace()
                userName = "Guest User"
                userEmail = "Failed to load"
                userAge = 0
                userGender = "male"
            }
        }
    }

    fun fetchUsageStats(userId: Int) {
        viewModelScope.launch {
            try {
                val response = api.getUsageStats(userId)
                usageDataPoints = response.data_points
                calculatePeakTime(response.data_points)
            } catch (e: Exception) {
                e.printStackTrace()
                usageDataPoints = List(24) { 0f }
                peakTimeLabel = "--:--"
            }
        }
    }

    fun recordUsage(userId: Int, startTimeMs: Long, endTimeMs: Long, durationSeconds: Int) {
        viewModelScope.launch {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val startTimeStr = sdf.format(Date(startTimeMs))
                val endTimeStr = sdf.format(Date(endTimeMs))

                val request = AppUsageCreate(
                    user_id = userId,
                    start_time = startTimeStr,
                    end_time = endTimeStr,
                    duration_seconds = durationSeconds
                )

                api.recordAppUsage(request)

                fetchUsageStats(userId)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}