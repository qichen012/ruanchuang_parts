package com.example.help_stu_agent.data.repo

import android.content.Context
import com.example.help_stu_agent.data.db.AppDatabase
import com.example.help_stu_agent.data.db.KnowledgeCardEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

class KnowledgeCardRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.knowledgeCardDao()

    fun observeAll(): Flow<List<KnowledgeCardEntity>> = dao.observeAll()

    suspend fun getById(id: String): KnowledgeCardEntity? = dao.getById(id)

    suspend fun deleteById(id: String) = dao.deleteById(id)

    suspend fun saveNewCard(
        rawJson: String,
        pdfDisplayName: String? = null,
        pdfUri: String? = null
    ): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()

        val parsed = runCatching { Json.parseToJsonElement(rawJson).jsonObject }.getOrNull()

        fun getStr(path1: String, path2: String): String? {
            val o1 = parsed?.get(path1)?.jsonObject ?: return null
            return o1[path2]?.jsonPrimitive?.contentOrNull
        }

        val colorHex = getStr("meta", "color")
        val category = getStr("meta", "category")
        val headerTitle = getStr("header", "title")
        val headerSubtitle = getStr("header", "subtitle")
        val footerQuote = getStr("footer", "quote")

        val entity = KnowledgeCardEntity(
            id = id,
            createdAt = now,
            pdfDisplayName = pdfDisplayName,
            pdfUri = pdfUri,
            category = category,
            colorHex = colorHex,
            headerTitle = headerTitle,
            headerSubtitle = headerSubtitle,
            footerQuote = footerQuote,
            rawJson = rawJson
        )

        dao.upsert(entity)
        return id
    }
}
