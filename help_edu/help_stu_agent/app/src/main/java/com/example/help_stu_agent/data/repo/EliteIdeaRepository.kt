package com.example.help_stu_agent.data.repo

import android.content.Context
import android.util.Log
import com.example.help_stu_agent.data.db.AppDatabase
import com.example.help_stu_agent.data.db.EliteIdeaEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import android.util.Base64

class EliteIdeaRepository(private val context: Context) {
    private val dao = AppDatabase.getInstance(context).eliteIdeaDao()

    fun observeAll(): Flow<List<EliteIdeaEntity>> = dao.observeAll()

    fun observeByDate(date: LocalDate): Flow<List<EliteIdeaEntity>> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return dao.observeByDate(startOfDay, endOfDay)
    }

    fun getReportDates(): Flow<List<String>> = dao.getReportDates()

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
                            true
                        } else {
                            caseCardIndex == targetCardIndex
                        }

                        if (matched) {
                            val caseTitle = caseObj.optString(
                                "case_title",
                                caseObj.optString("title", "")
                            )
                            val caseDescription = caseObj.optString(
                                "case_content",
                                caseObj.optString("description", "")
                            )
                            val imageUrl = caseObj.optString("image_url", "")
                            val imageDataUrl = caseObj.optString("image_data_url", "")

                            val localImagePath = saveImageDataUrlToLocal(
                                dataUrl = imageDataUrl,
                                key = "${title}_${targetCardIndex}_${caseTitle}_${k}"
                            )

                            val searchAndGenerate = caseObj.optJSONObject("search_and_generate")

                            val instanceNode = JSONObject().apply {
                                put("title", caseTitle)
                                put("description", caseDescription)
                                put("image_url", imageUrl)
                                put("local_image_path", localImagePath)
                                put("search_and_generate", searchAndGenerate ?: JSONObject())
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
                            instancesJson = instancesArray.toString(),
                            createdAt = System.currentTimeMillis() - entities.size
                        )
                    )
                }
            }

            if (entities.isNotEmpty()) {
                dao.insertAll(entities)
            }
        } catch (e: Exception) {
            Log.e("EliteIdeaDebug", "saveFromBackendJson error", e)
        }
    }

    private fun saveImageDataUrlToLocal(dataUrl: String, key: String): String {
        if (dataUrl.isBlank() || !dataUrl.startsWith("data:image")) return ""

        return try {
            val commaIndex = dataUrl.indexOf(",")
            if (commaIndex == -1) return ""

            val meta = dataUrl.substring(0, commaIndex)
            val base64Part = dataUrl.substring(commaIndex + 1)

            val ext = when {
                meta.contains("image/png") -> "png"
                meta.contains("image/jpeg") -> "jpg"
                meta.contains("image/webp") -> "webp"
                else -> "img"
            }

            val imageBytes = Base64.decode(base64Part, Base64.DEFAULT)

            val dir = File(context.filesDir, "elite_idea_images")
            if (!dir.exists()) dir.mkdirs()

            val fileName = "${md5(key)}.$ext"
            val outFile = File(dir, fileName)
            outFile.writeBytes(imageBytes)

            outFile.absolutePath
        } catch (e: Exception) {
            Log.e("EliteIdeaDebug", "saveImageDataUrlToLocal error", e)
            ""
        }
    }

    private fun md5(text: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
