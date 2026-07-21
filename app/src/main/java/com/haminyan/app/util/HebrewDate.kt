package com.haminyan.app.util

import android.icu.util.HebrewCalendar
import android.icu.util.ULocale
import java.util.Date

object HebrewDate {

    private val MONTH_NAMES = mapOf(
        HebrewCalendar.TISHRI to "תשרי",
        HebrewCalendar.HESHVAN to "חשוון",
        HebrewCalendar.KISLEV to "כסלו",
        HebrewCalendar.TEVET to "טבת",
        HebrewCalendar.SHEVAT to "שבט",
        HebrewCalendar.ADAR_1 to "אדר א׳",
        HebrewCalendar.ADAR to "אדר",
        HebrewCalendar.NISAN to "ניסן",
        HebrewCalendar.IYAR to "אייר",
        HebrewCalendar.SIVAN to "סיוון",
        HebrewCalendar.TAMUZ to "תמוז",
        HebrewCalendar.AV to "אב",
        HebrewCalendar.ELUL to "אלול",
    )

    /** תאריך עברי מלא של היום, למשל: "ו׳ אב תשפ״ו" */
    fun today(): String {
        val cal = HebrewCalendar(ULocale("he_IL@calendar=hebrew"))
        cal.time = Date()
        val day = toHebrewNumeral(cal.get(HebrewCalendar.DAY_OF_MONTH))
        val isLeap = isLeapYear(cal.get(HebrewCalendar.YEAR))
        val monthIdx = cal.get(HebrewCalendar.MONTH)
        val month = if (!isLeap && monthIdx == HebrewCalendar.ADAR_1) "אדר"
        else MONTH_NAMES[monthIdx] ?: ""
        val year = toHebrewNumeral(cal.get(HebrewCalendar.YEAR) % 1000)
        return "$day $month $year"
    }

    /** שנה מעוברת לפי מחזור 19 השנים */
    private fun isLeapYear(year: Int): Boolean = (7 * year + 1) % 19 < 7

    private fun toHebrewNumeral(value: Int): String {
        var n = value
        val letters = StringBuilder()
        val hundreds = listOf(400 to "ת", 300 to "ש", 200 to "ר", 100 to "ק")
        val tens = listOf(90 to "צ", 80 to "פ", 70 to "ע", 60 to "ס", 50 to "נ", 40 to "מ", 30 to "ל", 20 to "כ", 10 to "י")
        val units = listOf(9 to "ט", 8 to "ח", 7 to "ז", 6 to "ו", 5 to "ה", 4 to "ד", 3 to "ג", 2 to "ב", 1 to "א")

        for ((v, l) in hundreds) while (n >= v) { letters.append(l); n -= v }
        // ט"ו וט"ז במקום י-ה / י-ו
        if (n == 15) { letters.append("טו"); n = 0 }
        if (n == 16) { letters.append("טז"); n = 0 }
        for ((v, l) in tens) if (n >= v) { letters.append(l); n -= v }
        for ((v, l) in units) if (n >= v) { letters.append(l); n -= v }

        return when {
            letters.length >= 2 -> letters.insert(letters.length - 1, '״').toString()
            letters.length == 1 -> "$letters׳"
            else -> letters.toString()
        }
    }
}
