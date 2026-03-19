package com.example.help_stu_agent.data.repo

import com.example.help_stu_agent.data.db.MeetingMinutesDao
import com.example.help_stu_agent.data.db.MeetingMinutesEntity
import kotlinx.coroutines.flow.Flow

class MeetingMinutesRepository(private val dao: MeetingMinutesDao) {

    // 监听所有会议纪要
    fun observeAll(): Flow<List<MeetingMinutesEntity>> = dao.observeAll()

    // 监听指定用户的会议纪要
    fun observeByUserId(userId: Int): Flow<List<MeetingMinutesEntity>> = dao.observeByUserId(userId)

    // 获取所有会议纪要
    suspend fun getAll(): List<MeetingMinutesEntity> = dao.getAll()

    // 按ID获取会议纪要
    suspend fun getById(id: String): MeetingMinutesEntity? = dao.getById(id)

    // 获取指定用户的最近记录
    suspend fun getRecentByUserId(userId: Int, limit: Int = 10): List<MeetingMinutesEntity> {
        return dao.getRecentByUserId(userId, limit)
    }

    // 保存或更新会议纪要
    suspend fun save(entity: MeetingMinutesEntity) {
        dao.upsert(entity)
    }

    // 批量保存
    suspend fun saveAll(entities: List<MeetingMinutesEntity>) {
        entities.forEach { dao.upsert(it) }
    }

    // 搜索会议纪要
    suspend fun search(keyword: String): List<MeetingMinutesEntity> {
        return if (keyword.isBlank()) {
            dao.getAll()
        } else {
            dao.search(keyword)
        }
    }

    // 删除会议纪要
    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    // 删除用户的所有会议纪要
    suspend fun deleteByUserId(userId: Int) {
        dao.deleteByUserId(userId)
    }

    // 清空所有会议纪要
    suspend fun clearAll() {
        dao.clearAll()
    }

    // 删除旧记录（清理空间）
    suspend fun deleteOlderThan(beforeTime: Long) {
        dao.deleteOlderThan(beforeTime)
    }
}
