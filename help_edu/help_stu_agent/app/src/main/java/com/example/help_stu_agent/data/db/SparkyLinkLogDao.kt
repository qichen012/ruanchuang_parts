package com.example.help_stu_agent.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SparkyLinkLogDao {
    @Insert
    suspend fun insert(log: SparkyLinkLogEntity)

    @Query("SELECT DISTINCT strftime('%Y-%m-%d', createdAt / 1000, 'unixepoch') FROM sparky_link_logs WHERE userId = :userId ORDER BY createdAt DESC")
    fun getLogDates(userId: Int): Flow<List<String>>

    @Query("SELECT * FROM sparky_link_logs WHERE userId = :userId AND strftime('%Y-%m-%d', createdAt / 1000, 'unixepoch') = :date")
    suspend fun getLogsByDate(userId: Int, date: String): List<SparkyLinkLogEntity>
}
