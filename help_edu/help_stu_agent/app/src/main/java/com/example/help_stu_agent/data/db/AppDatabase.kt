package com.example.help_stu_agent.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        KnowledgeCardEntity::class,
        KnowledgeTreeEntity::class,
        EliteIdeaEntity::class,
        MeetingMinutesEntity::class,
        PhotoLogEntity::class,
        SparkyLinkLogEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun knowledgeCardDao(): KnowledgeCardDao
    abstract fun knowledgeTreeDao(): KnowledgeTreeDao
    abstract fun eliteIdeaDao(): EliteIdeaDao
    abstract fun meetingMinutesDao(): MeetingMinutesDao
    abstract fun photoLogDao(): PhotoLogDao
    abstract fun sparkyLinkLogDao(): SparkyLinkLogDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val inst = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "help_stu_agent.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = inst
                inst
            }
        }
    }
}
