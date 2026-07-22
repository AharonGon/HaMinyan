package com.haminyan.app.data

import com.google.gson.annotations.SerializedName

data class ZmanimResponse(
    @SerializedName("date") val date: String? = null,
    @SerializedName("location") val location: ZmanimLocation? = null,
    @SerializedName("times") val times: Map<String, String>? = null,
)

data class ZmanimLocation(
    @SerializedName("title") val title: String? = null,
    @SerializedName("latitude") val latitude: Double? = null,
    @SerializedName("longitude") val longitude: Double? = null,
)

data class CalendarResponse(
    @SerializedName("items") val items: List<CalendarItem>? = null,
)

data class CalendarItem(
    @SerializedName("title") val title: String? = null,
    @SerializedName("hebrew") val hebrew: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("subcat") val subcat: String? = null,
    @SerializedName("memo") val memo: String? = null,
    @SerializedName("link") val link: String? = null,
)

/** זמן הלכה אחד לתצוגה במסך */
data class HalachicTime(
    val key: String,
    val label: String,
    val time: String,
    val group: ZmanimGroup,
)

enum class ZmanimGroup(val title: String) {
    MORNING("בוקר"),
    DAY("יום"),
    EVENING("ערב"),
}

/** מידע יומי מרוכז לתצוגה */
data class JewishDayInfo(
    val dafYomi: String? = null,
    val dafYomiLink: String? = null,
    val specialDays: List<SpecialDay> = emptyList(),
)

data class SpecialDay(
    val title: String,
    val category: String,
    val memo: String? = null,
    val link: String? = null,
)
