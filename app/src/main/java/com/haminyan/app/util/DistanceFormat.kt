package com.haminyan.app.util

import kotlin.math.roundToInt

object DistanceFormat {

    /** "650 מ׳" או "1.2 ק״מ" */
    fun meters(value: Double?): String? {
        if (value == null || value < 0) return null
        return if (value < 1000) {
            "${value.roundToInt()} מ׳"
        } else {
            val km = value / 1000.0
            val text = if (km < 10) String.format("%.1f", km) else km.roundToInt().toString()
            "$text ק״מ"
        }
    }

    /** זמן הליכה משוער: "8 דק׳ הליכה" / "פחות מדקה הליכה" / "1:05 ש׳ הליכה" */
    fun walkDuration(seconds: Double?): String? {
        if (seconds == null || seconds < 0) return null
        val minutes = (seconds / 60.0).roundToInt()
        return when {
            minutes < 1 -> "פחות מדקה הליכה"
            minutes < 60 -> "$minutes דק׳ הליכה"
            else -> "${minutes / 60}:${"%02d".format(minutes % 60)} ש׳ הליכה"
        }
    }
}
