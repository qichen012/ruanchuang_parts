package com.example.help_stu_agent.data.repo

import android.content.Context
import android.util.Log
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
            Log.d("EliteIdeaDebug", "rootArray.length=${rootArray.length()}")

            for (i in 0 until rootArray.length()) {
                val payload = rootArray.optJSONObject(i) ?: continue

                val cardsArray = payload.optJSONArray("elite_idea_cards") ?: JSONArray()
                val casesArray = payload.optJSONArray("elite_idea_cases") ?: JSONArray()

                Log.d("EliteIdeaDebug", "cards=${cardsArray.length()}, cases=${casesArray.length()}")

                for (j in 0 until cardsArray.length()) {
                    val card = cardsArray.optJSONObject(j) ?: continue

                    val category = card.optString("origin_concept", "WISDOM")
                    val title = card.optString(
                        "meta_idea_name",
                        card.optString("title", "Unknown Title")
                    )
                    val description = card.optString(
                        "meta_explanation",
                        card.optString("description", "")
                    )

                    val targetCardIndex = when (val raw = card.opt("card_index")) {
                        is Number -> raw.toInt()
                        is String -> raw.toIntOrNull() ?: -1
                        else -> -1
                    }

                    val instancesArray = JSONArray()

                    for (k in 0 until casesArray.length()) {
                        val caseObj = casesArray.optJSONObject(k) ?: continue

                        val caseCardIndex = when (val raw = caseObj.opt("card_index")) {
                            is Number -> raw.toInt()
                            is String -> raw.toIntOrNull() ?: -1
                            else -> -1
                        }

                        val matched = if (targetCardIndex == -1) {
                            // 没有关联字段时，先全部挂上，保证前端能显示
                            true
                        } else {
                            caseCardIndex == targetCardIndex
                        }

                        if (matched) {
                            val instanceNode = JSONObject().apply {
                                put(
                                    "title",
                                    caseObj.optString(
                                        "case_title",
                                        caseObj.optString("title", "")
                                    )
                                )
                                put(
                                    "description",
                                    caseObj.optString(
                                        "case_content",
                                        caseObj.optString("description", "")
                                    )
                                )
                                put("image_url", caseObj.optString("image_url", ""))
                                put("image_data_url", caseObj.optString("image_data_url", ""))
                            }
                            instancesArray.put(instanceNode)
                        }
                    }

                    Log.d(
                        "EliteIdeaDebug",
                        "card[$j] title=$title, targetCardIndex=$targetCardIndex, matched=${instancesArray.length()}"
                    )

                    entities.add(
                        EliteIdeaEntity(
                            id = UUID.randomUUID().toString(),
                            category = category,
                            title = title,
                            description = description,
                            instancesJson = instancesArray.toString(),
                            createdAt = System.currentTimeMillis() - entities.size
                        )
                    )
                }
            }

            dao.clearAll()
            if (entities.isNotEmpty()) {
                dao.insertAll(entities)
            }
        } catch (e: Exception) {
            Log.e("EliteIdeaDebug", "saveFromBackendJson error", e)
        }
    }
}