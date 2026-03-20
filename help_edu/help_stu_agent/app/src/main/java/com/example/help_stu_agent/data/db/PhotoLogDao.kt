package com.example.help_stu_agent.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoLogDao {
    @Insert
    suspend fun insert(log: PhotoLogEntity)

    @Query("SELECT DISTINCT strftime('%Y-%m-%d', createdAt / 1000, 'unixepoch') FROM photo_logs WHERE userId = :userId ORDER BY createdAt DESC")
    fun getUploadedDates(userId: Int): Flow<List<String>>

    @Query("SELECT * FROM photo_logs WHERE userId = :userId AND strftime('%Y-%m-%d', createdAt / 1000, 'unixepoch') = :date")
    suspend fun getPhotosByDate(userId: Int, date: String): List<PhotoLogEntity>
}
