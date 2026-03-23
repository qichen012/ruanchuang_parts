package com.example.help_stu_agent.ui.sparkyLink

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

fun buildMonthCells(month: YearMonth): List<CalendarCell> {
    val firstDay = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()

    val firstDayIndex = when (firstDay.dayOfWeek) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
        null -> 0
    }

    val result = mutableListOf<CalendarCell>()

    repeat(firstDayIndex) {
        result.add(CalendarCell(date = null, isCurrentMonth = false))
    }

    for (day in 1..daysInMonth) {
        result.add(CalendarCell(date = month.atDay(day), isCurrentMonth = true))
    }

    while (result.size % 7 != 0) {
        result.add(CalendarCell(date = null, isCurrentMonth = false))
    }

    while (result.size < 42) {
        result.add(CalendarCell(date = null, isCurrentMonth = false))
    }

    return result
}

fun parseToLocalDate(dateString: String): LocalDate? {
    val normalized = dateString.trim()
        .replace("/", "-")
        .replace(".", "-")

    val patterns = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("yyyy-M-d"),
        DateTimeFormatter.ofPattern("yyyy-MM-d"),
        DateTimeFormatter.ofPattern("yyyy-M-dd")
    )

    patterns.forEach { formatter ->
        try {
            return LocalDate.parse(normalized, formatter)
        } catch (_: Exception) {
        }
    }

    return null
}

fun formatDisplayDate(date: String): String {
    val cleaned = date.trim()
    if (cleaned.isBlank()) return "--.--"

    val normalized = cleaned.replace("/", "-").replace(".", "-")
    val parts = normalized.split("-").filter { it.isNotBlank() }

    return when {
        parts.size >= 3 -> {
            val month = parts[1].padStart(2, '0')
            val day = parts[2].padStart(2, '0')
            "$month.$day"
        }
        parts.size == 2 -> {
            val month = parts[0].padStart(2, '0')
            val day = parts[1].padStart(2, '0')
            "$month.$day"
        }
        else -> cleaned
    }
}
