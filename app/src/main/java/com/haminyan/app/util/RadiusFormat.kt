package com.haminyan.app.util

object RadiusFormat {

  val OPTIONS_METERS = listOf(300, 500, 750, 1_000, 2_000, 5_000, 10_000, 15_000)

  fun label(meters: Int): String = when {
    meters < 1_000 -> "$meters מ׳"
    meters % 1_000 == 0 -> "${meters / 1_000} ק״מ"
    else -> String.format("%.1f ק״מ", meters / 1_000.0)
  }

  /** רדיוס מינימלי לבקשת API (בק"מ, שלם) */
  fun apiRadiusKm(meters: Int): Int = maxOf(1, (meters + 999) / 1_000)
}
