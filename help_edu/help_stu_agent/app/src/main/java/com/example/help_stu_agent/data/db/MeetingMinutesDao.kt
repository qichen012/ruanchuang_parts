package com.example.help_stu_agent.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingMinutesDao {

    // 插入或更新
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MeetingMinutesEntity)

    // 监听所有记录（按时间倒序）
    @Query("SELECT * FROM meeting_minutes ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MeetingMinutesEntity>>

    // 监听指定用户的记录
    @Query("SELECT * FROM meeting_minutes WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeByUserId(userId: Int): Flow<List<MeetingMinutesEntity>>

    // 获取所有记录
    @Query("SELECT * FROM meeting_minutes ORDER BY createdAt DESC")
    suspend fun getAll(): List<MeetingMinutesEntity>

    // 获取指定ID的记录
    @Query("SELECT * FROM meeting_minutes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MeetingMinutesEntity?

    // 获取指定用户的最近N条记录
    @Query("SELECT * FROM meeting_minutes WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentByUserId(userId: Int, limit: Int = 10): List<MeetingMinutesEntity>

    // 搜索
    @Query("""
        SELECT * FROM meeting_minutes 
        WHERE (courseName LIKE '%' || :keyword || '%' 
            OR topic LIKE '%' || :keyword || '%'
            OR summary LIKE '%' || :keyword || '%')
        ORDER BY createdAt DESC
    """)
    suspend fun search(keyword: String): List<MeetingMinutesEntity>

    // 删除指定ID的记录
    @Query("DELETE FROM meeting_minutes WHERE id = :id")
    suspend fun deleteById(id: String)

    // 删除指定用户的所有记录
    @Query("DELETE FROM meeting_minutes WHERE userId = :userId")
    suspend fun deleteByUserId(userId: Int)

    // 删除所有记录
    @Query("DELETE FROM meeting_minutes")
    suspend fun clearAll()

    // 删除指定时间之前的旧记录（用于清理空间）
    @Query("DELETE FROM meeting_minutes WHERE createdAt < :beforeTime")
    suspend fun deleteOlderThan(beforeTime: Long)

    // 批量删除
    @Delete
    suspend fun delete(vararg entities: MeetingMinutesEntity)
}
