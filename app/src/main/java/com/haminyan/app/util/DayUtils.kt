package com.haminyan.app.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object DayUtils {

    /** אותיות הימים כפי שמגיעות מהשרת: א=ראשון ... ו=שישי, ש=שבת */
    val DAY_LETTERS = listOf('א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ש')

    val DAY_NAMES = mapOf(
        'א' to "ראשון",
        'ב' to "שני",
        'ג' to "שלישי",
        'ד' to "רביעי",
        'ה' to "חמישי",
        'ו' to "שישי",
        'ש' to "שבת",
    )

    fun todayLetter(date: LocalDate = LocalDate.now()): Char = when (date.dayOfWeek) {
        DayOfWeek.SUNDAY -> 'א'
        DayOfWeek.MONDAY -> 'ב'
        DayOfWeek.TUESDAY -> 'ג'
        DayOfWeek.WEDNESDAY -> 'ד'
        DayOfWeek.THURSDAY -> 'ה'
        DayOfWeek.FRIDAY -> 'ו'
        DayOfWeek.SATURDAY -> 'ש'
    }

    fun isActiveOnDay(days: String?, dayLetter: Char): Boolean =
        days.isNullOrBlank() || days.contains(dayLetter)

    fun parseTime(time: String?): LocalTime? = runCatching {
        LocalTime.parse(time?.trim(), DateTimeFormatter.ofPattern("H:mm"))
    }.getOrNull()

    /** תיאור ימים ידידותי: "כל השבוע", "א-ה", או רשימת ימים */
    fun daysLabel(days: String?): String {
        if (days.isNullOrBlank()) return "כל יום"
        val set = days.toSet()
        return when {
            DAY_LETTERS.all { it in set } -> "כל השבוע"
            set == setOf('א', 'ב', 'ג', 'ד', 'ה') -> "ימים א׳-ה׳"
            set == setOf('א', 'ב', 'ג', 'ד', 'ה', 'ו') -> "ימים א׳-ו׳"
            set.size == 1 -> "יום ${DAY_NAMES[set.first()] ?: days}"
            else -> days.toCharArray().joinToString("׳, ", postfix = "׳")
        }
    }

    /** דקות שנותרו עד שעה נתונה היום, או null אם עברה */
    fun minutesUntil(time: String?, now: LocalTime = LocalTime.now()): Long? {
        val t = parseTime(time) ?: return null
        if (t.isBefore(now)) return null
        return java.time.Duration.between(now, t).toMinutes()
    }

    fun countdownLabel(minutes: Long): String = when {
        minutes < 1 -> "עכשיו"
        minutes < 60 -> "בעוד $minutes דק׳"
        else -> "בעוד ${minutes / 60}:${"%02d".format(minutes % 60)} ש׳"
    }
}
