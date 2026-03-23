package com.example.help_stu_agent.data.repo

import android.content.Context
import com.example.help_stu_agent.data.db.DbProvider
import com.example.help_stu_agent.data.db.KnowledgeTreeEntity
import java.io.File
import java.util.UUID

class KnowledgeTreeRepository(private val context: Context) {

    private val dao = DbProvider.get(context).knowledgeTreeDao()

    suspend fun saveNewTree(
        pdfDisplayName: String?,
        pdfUri: String?,
        title: String?,
        jsonString: String
    ): String {
        val id = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()

        val dir = File(context.filesDir, "knowledge_trees")
        if (!dir.exists()) dir.mkdirs()

        val jsonFile = File(dir, "$id.json")
        jsonFile.writeText(jsonString, Charsets.UTF_8)

        val entity = KnowledgeTreeEntity(
            id = id,
            title = title?.takeIf { it.isNotBlank() } ?: (pdfDisplayName ?: "Knowledge Tree"),
            createdAt = createdAt,
            pdfDisplayName = pdfDisplayName,
            pdfUri = pdfUri,
            jsonPath = jsonFile.absolutePath
        )
        dao.upsert(entity)
        return id
    }

    suspend fun listAll(): List<KnowledgeTreeEntity> = dao.listAll()

    suspend fun loadJsonById(id: String): String? {
        val e = dao.getById(id) ?: return null
        val f = File(e.jsonPath)
        return if (f.exists()) f.readText(Charsets.UTF_8) else null
    }

}
