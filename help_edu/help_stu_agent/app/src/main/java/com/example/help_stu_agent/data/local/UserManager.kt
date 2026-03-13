package com.example.help_stu_agent.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserManager(private val context: Context) {
    companion object {
        val USER_ID_KEY = intPreferencesKey("USER_ID")
        val TOKEN_KEY = stringPreferencesKey("TOKEN")
    }

    // 监听 User ID
    val userIdFlow: Flow<Int?> = context.dataStore.data.map { it[USER_ID_KEY] }

    // 监听 Token
    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[TOKEN_KEY] }

    // 同时保存 User ID 和 Token
    suspend fun saveUserSession(userId: Int, token: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
            preferences[TOKEN_KEY] = token
        }
    }

    // 合并为一个方法：退出登录时清空所有用户数据
    suspend fun clearUserSession() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_ID_KEY)
            preferences.remove(TOKEN_KEY)
        }
    }
}