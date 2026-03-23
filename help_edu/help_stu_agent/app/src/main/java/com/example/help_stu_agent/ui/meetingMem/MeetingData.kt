package com.example.help_stu_agent.ui.meetingMem

import com.example.help_stu_agent.data.db.MeetingMinutesEntity
import org.json.JSONArray
import org.json.JSONObject

data class MeetingMinutes(
    val topic: String = "",
    val summary: String = "",
    val points: List<String> = emptyList(),
    val conclusions: List<String> = emptyList(),
    val todos: List<String> = emptyList(),
    val rawText: String = ""
) {
    fun toPlainText(): String = buildString {
        appendLine("会议纪要")
        appendLine()

        appendLine("会议主题：")
        appendLine(topic.ifBlank { "（无）" })
        appendLine()

        appendLine("摘要：")
        appendLine(summary.ifBlank { "（无）" })
        appendLine()

        appendLine("核心要点：")
        if (points.isEmpty()) {
            appendLine("（无）")
        } else {
            points.forEach { appendLine("- $it") }
        }
        appendLine()

        appendLine("达成结论：")
        if (conclusions.isEmpty()) {
            appendLine("（无）")
        } else {
            conclusions.forEach { appendLine("- $it") }
        }
        appendLine()

        appendLine("待办事项：")
        if (todos.isEmpty()) {
            appendLine("（无）")
        } else {
            todos.forEach { appendLine("- $it") }
        }
        appendLine()

        appendLine("原始文本：")
        appendLine(rawText.ifBlank { "（无）" })
    }
}

fun MeetingMinutesEntity.toMeetingMinutes(): MeetingMinutes {
    if (minutesMarkdown.isNotBlank()) {
        return parseMeetingMinutesMarkdown(minutesMarkdown)
    }

    val pointsList = runCatching {
        val arr = JSONArray(pointsJson)
        List(arr.length()) { arr.getString(it) }
    }.getOrDefault(emptyList())

    val todosList = runCatching {
        val arr = JSONArray(todosJson)
        List(arr.length()) { arr.getString(it) }
    }.getOrDefault(emptyList())

    return MeetingMinutes(
        topic = topic,
        summary = summary,
        points = pointsList,
        conclusions = emptyList(),
        todos = todosList,
        rawText = rawText
    )
}

fun parseMeetingMinutesMarkdown(raw: String): MeetingMinutes {
    val t = raw.trim()
    if (t.isBlank()) return MeetingMinutes()

    val lines = t.lines().map { it.trimEnd() }

    var topic = ""
    val points = mutableListOf<String>()
    val conclusions = mutableListOf<String>()
    val todos = mutableListOf<String>()

    var mode: String? = null

    // 当前正在拼接的一个核心观点块
    var currentPointBlock: StringBuilder? = null

    fun cleanInlineMarkdown(s: String): String {
        return s
            .replace("**", "")
            .replace("__", "")
            .replace(Regex("""^\s*[-*]\s+"""), "")
            .replace(Regex("""^\s*\d+\.\s*"""), "")
            .trim()
    }

    fun isTopLevelNumberedItem(s: String): Boolean {
        return s.matches(Regex("""^\s*\d+\.\s+.*"""))
    }

    fun isBulletSubItem(s: String): Boolean {
        return s.matches(Regex("""^\s*[*-]\s+.*"""))
    }

    fun flushPointBlock() {
        val text = currentPointBlock?.toString()?.trim().orEmpty()
        if (text.isNotBlank()) {
            points += text
        }
        currentPointBlock = null
    }

    for (rawLine in lines) {
        val ln = rawLine.trim()
        if (ln.isBlank()) continue

        when {
            ln.startsWith("## 会议主题") -> {
                flushPointBlock()
                mode = "topic"
                continue
            }

            ln.startsWith("## 核心观点") -> {
                flushPointBlock()
                mode = "points"
                continue
            }

            ln.startsWith("## 达成的结论") -> {
                flushPointBlock()
                mode = "conclusions"
                continue
            }

            ln.startsWith("## 待办事项") -> {
                flushPointBlock()
                mode = "todos"
                continue
            }

            ln.startsWith("# ") ||
                    ln.startsWith("---") ||
                    ln.startsWith("*纪要整理人") ||
                    ln.startsWith("*整理日期") -> {
                continue
            }
        }

        when (mode) {
            "topic" -> {
                if (topic.isBlank()) {
                    topic = cleanInlineMarkdown(ln)
                }
            }

            "points" -> {
                val cleaned = cleanInlineMarkdown(ln)
                if (cleaned.isBlank()) continue

                when {
                    // 新的一级编号条目：先提交上一个 block，再开一个新 block
                    isTopLevelNumberedItem(ln) -> {
                        flushPointBlock()
                        currentPointBlock = StringBuilder(cleaned)
                    }

                    // 子项目：追加到当前 block
                    isBulletSubItem(ln) -> {
                        if (currentPointBlock == null) {
                            currentPointBlock = StringBuilder("• $cleaned")
                        } else {
                            currentPointBlock!!.append("\n• ").append(cleaned)
                        }
                    }

                    // 忽略说话人标签
                    ln.startsWith("**【") || ln.startsWith("【") -> {
                        // do nothing
                    }

                    // 其他普通行：继续拼到当前 block
                    else -> {
                        if (currentPointBlock == null) {
                            currentPointBlock = StringBuilder(cleaned)
                        } else {
                            currentPointBlock!!.append("\n").append(cleaned)
                        }
                    }
                }
            }

            "conclusions" -> {
                val cleaned = cleanInlineMarkdown(ln)
                if (cleaned.isNotBlank()) conclusions += cleaned
            }

            "todos" -> {
                val cleaned = cleanInlineMarkdown(ln)
                if (cleaned.isNotBlank()) todos += cleaned
            }
        }
    }

    flushPointBlock()

    val summary = buildString {
        if (topic.isNotBlank()) {
            append("本次会议主题为“")
            append(topic)
            append("”。")
        }
        if (points.isNotEmpty()) {
            append("会议围绕")
            append(points.take(3).joinToString("；"))
            append("等核心内容展开。")
        }
        if (todos.isNotEmpty()) {
            append("并形成了后续待办安排。")
        }
    }.trim()

    return MeetingMinutes(
        topic = topic,
        summary = summary,
        points = points.distinct(),
        conclusions = conclusions.distinct(),
        todos = todos.distinct(),
        rawText = t
    )
}
