package com.example.help_stu_agent.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(
    entities = [
        KnowledgeCardEntity::class,
        KnowledgeTreeEntity::class
    ],
    version = 2, // 你如果原来是 1，这里必须改成 2
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun knowledgeCardDao(): KnowledgeCardDao

    abstract fun knowledgeTreeDao(): KnowledgeTreeDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val inst = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "help_stu_agent.db"
                )
                    // 开发阶段强烈建议：先用它避免迁移麻烦
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = inst
                inst
            }
        }
    }
}
