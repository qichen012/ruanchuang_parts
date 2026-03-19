package com.example.help_stu_agent.ui.meetingMem

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.help_stu_agent.data.db.AppDatabase
import com.example.help_stu_agent.data.db.MeetingMinutesEntity
import com.example.help_stu_agent.data.local.UserManager
import com.example.help_stu_agent.data.repo.MeetingMinutesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MeetingViewModel(
    private val repository: MeetingMinutesRepository,
    private val userManager: UserManager
) : ViewModel() {

    // 监听当前用户的会议纪要
    val userMeetings: Flow<List<MeetingMinutesEntity>> = repository.observeAll()

    // 最新的会议纪要
    val latestMeeting: StateFlow<MeetingMinutesEntity?> = repository.observeAll()
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 获取指定ID的会议纪要
    suspend fun getMeetingById(id: String): MeetingMinutesEntity? {
        return repository.getById(id)
    }

    // 搜索会议纪要
    suspend fun search(keyword: String): List<MeetingMinutesEntity> {
        return repository.search(keyword)
    }

    // 删除会议纪要
    fun deleteMeeting(id: String) {
        viewModelScope.launch {
            val meeting = repository.getById(id)
            repository.delete(id)
            // 删除本地音频文件
            deleteAudioFile(meeting?.audioLocalPath)
        }
    }

    private fun deleteAudioFile(filePath: String?) {
        if (filePath.isNullOrBlank()) return
        try {
            val file = java.io.File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(context)
                    val repository = MeetingMinutesRepository(db.meetingMinutesDao())
                    val userManager = UserManager(context)
                    @Suppress("UNCHECKED_CAST")
                    return MeetingViewModel(repository, userManager) as T
                }
            }
        }
    }
}
