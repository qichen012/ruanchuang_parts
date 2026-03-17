package com.example.help_stu_agent.data.repo

import android.content.Context
import com.example.help_stu_agent.data.db.AppDatabase
import com.example.help_stu_agent.data.db.EliteIdeaEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class EliteIdeaRepository(context: Context) {
    private val dao = AppDatabase.getInstance(context).eliteIdeaDao()

    fun observeAll(): Flow<List<EliteIdeaEntity>> = dao.observeAll()

    suspend fun saveFromBackendJson(jsonArrayString: String) {
        val entities = mutableListOf<EliteIdeaEntity>()

        try {
            val rootArray = JSONArray(jsonArrayString)

            for (i in 0 until rootArray.length()) {
                val payload = rootArray.optJSONObject(i) ?: continue

                // 1. 分别提取 cards 数组和 cases 数组
                val cardsArray = payload.optJSONArray("elite_idea_cards") ?: continue
                val casesArray = payload.optJSONArray("elite_idea_cases") ?: JSONArray()

                for (j in 0 until cardsArray.length()) {
                    val card = cardsArray.optJSONObject(j) ?: continue

                    val category = card.optString("origin_concept", "WISDOM")
                    val title = card.optString("meta_idea_name", "Unknown Title")
                    val description = card.optString("meta_explanation", "")

                    // 获取当前卡片的索引 ID
                    val targetCardIndex = card.optInt("card_index", -1)

                    // 构建当前卡片对应的 instancesArray
                    val instancesArray = JSONArray()

                    // 遍历所有的 cases，找出属于当前卡片的 case
                    for (k in 0 until casesArray.length()) {
                        val caseObj = casesArray.optJSONObject(k) ?: continue
                        val caseCardIndex = caseObj.optInt("card_index", -1)

                        // 如果 card_index 匹配，说明这个案例属于当前卡片
                        if (caseCardIndex == targetCardIndex && targetCardIndex != -1) {
                            val instanceNode = JSONObject().apply {
                                // 映射为前端 UI 期望的字段名 title 和 description
                                put("title", caseObj.optString("case_title", ""))
                                put("description", caseObj.optString("case_content", ""))
                            }
                            instancesArray.put(instanceNode)
                        }
                    }

                    entities.add(
                        EliteIdeaEntity(
                            id = UUID.randomUUID().toString(),
                            category = category,
                            title = title,
                            description = description,
                            instancesJson = instancesArray.toString(), // 存入匹配好的 JSON 字符串
                            createdAt = System.currentTimeMillis() - entities.size
                        )
                    )
                }
            }

            // 更新数据库
            if (entities.isNotEmpty()) {
                dao.clearAll()
                dao.insertAll(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}