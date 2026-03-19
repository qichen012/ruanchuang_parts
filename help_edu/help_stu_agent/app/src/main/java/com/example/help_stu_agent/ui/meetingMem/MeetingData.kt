package com.example.help_stu_agent.ui.meetingMem

import com.example.help_stu_agent.data.db.MeetingMinutesEntity
import org.json.JSONArray
import org.json.JSONObject

data class MeetingMinutes(
    val summary: String = "",
    val points: List<String> = emptyList(),
    val todos: List<String> = emptyList(),
    val rawText: String = ""
) {
    fun toPlainText(): String = buildString {
        appendLine("会议纪要")
        appendLine()
        appendLine("摘要：")
        appendLine(summary.ifBlank { "（无）" })
        appendLine()
        appendLine("核心要点：")
        if (points.isEmpty()) appendLine("（无）") else points.forEach { appendLine("- $it") }
        appendLine()
        appendLine("待办事项：")
        if (todos.isEmpty()) appendLine("（无）") else todos.forEach { appendLine("- $it") }
        appendLine()
        appendLine("原始文本：")
        appendLine(rawText.ifBlank { "（无）" })
    }
}

fun MeetingMinutesEntity.toMeetingMinutes(): MeetingMinutes {
    val pointsList = runCatching {
        val arr = JSONArray(pointsJson)
        List(arr.length()) { arr.getString(it) }
    }.getOrDefault(emptyList())

    val todosList = runCatching {
        val arr = JSONArray(todosJson)
        List(arr.length()) { arr.getString(it) }
    }.getOrDefault(emptyList())

    return MeetingMinutes(
        summary = summary,
        points = pointsList,
        todos = todosList,
        rawText = rawText
    )
}

fun parseMeetingMinutes(raw: String): MeetingMinutes {
    val t = raw.trim()
    if (t.isBlank()) return MeetingMinutes()

    // 1) JSON 优先
    runCatching {
        val jo = JSONObject(t)
        val rawText = jo.optString("text").ifBlank { jo.optString("raw_text") }
        val summary = jo.optString("summary").ifBlank {
            jo.optString("摘要").ifBlank { jo.optString("minutes_summary") }
        }
        val points = extractStringList(jo, "points", "core_points", "核心要点", "key_points")
        val todos = extractStringList(jo, "todos", "待办事项", "todo", "action_items")
        return MeetingMinutes(summary = summary, points = points, todos = todos, rawText = rawText)
    }

    // 2) 纯文本后备解析方案
    val lines = t.lines().map { it.trim() }.filter { it.isNotBlank() }
    fun stripBullet(s: String): String = s.removePrefix("-").removePrefix("•").removePrefix("·").trim()

    var summary = ""
    val points = mutableListOf<String>()
    val todos = mutableListOf<String>()
    var mode: String? = null
    val sb = StringBuilder()

    for (ln in lines) {
        val lower = ln.lowercase()
        when {
            ln.contains("摘要") || lower.startsWith("summary") -> {
                if (mode == "summary" && sb.isNotBlank() && summary.isBlank()) summary = sb.toString().trim()
                mode = "summary"
                sb.clear()
                val after = ln.substringAfter("摘要", "").trim().trimStart('：', ':').trim()
                if (after.isNotBlank()) sb.append(after)
            }
            ln.contains("核心要点") || ln.contains("要点") || lower.startsWith("key points") -> {
                if (mode == "summary" && summary.isBlank()) summary = sb.toString().trim()
                mode = "points"
            }
            ln.contains("待办") || lower.startsWith("action items") || lower.startsWith("todo") -> {
                if (mode == "summary" && summary.isBlank()) summary = sb.toString().trim()
                mode = "todos"
            }
            else -> {
                when (mode) {
                    "summary" -> {
                        if (sb.isNotEmpty()) sb.append('\n')
                        sb.append(ln)
                    }
                    "points" -> stripBullet(ln).takeIf { it.isNotBlank() }?.let { points += it }
                    "todos" -> stripBullet(ln).takeIf { it.isNotBlank() }?.let { todos += it }
                    else -> if (summary.isBlank()) summary = ln else points += stripBullet(ln)
                }
            }
        }
    }
    if (mode == "summary" && summary.isBlank()) summary = sb.toString().trim()

    return MeetingMinutes(summary = summary, points = points.distinct(), todos = todos.distinct(), rawText = t)
}

fun extractStringList(jo: JSONObject, vararg keys: String): List<String> {
    for (k in keys) {
        if (!jo.has(k)) continue
        when (val v = jo.get(k)) {
            is JSONArray -> {
                val out = mutableListOf<String>()
                for (i in 0 until v.length()) out += v.optString(i)
                return out.filter { it.isNotBlank() }
            }
            is String -> {
                return v.lines().map { it.trim() }.filter { it.isNotBlank() }
                    .map { it.removePrefix("-").removePrefix("•").trim() }
            }
        }
    }
    return emptyList()
}