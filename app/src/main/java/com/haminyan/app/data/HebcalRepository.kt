package com.haminyan.app.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone
import kotlin.math.roundToInt

class HebcalRepository(private val api: HebcalApi) {

    private val zmanimCache = TtlCache<String, List<HalachicTime>>(ttlMillis = 6 * 60 * 60_000L)
    private val dayInfoCache = TtlCache<String, JewishDayInfo>(ttlMillis = 6 * 60 * 60_000L)

    suspend fun zmanim(lat: Double, lng: Double, date: LocalDate = LocalDate.now()): List<HalachicTime> {
        val key = zmanimKey(lat, lng, date)
        zmanimCache.get(key)?.let { return it }

        val tzid = TimeZone.getDefault().id
        val response = api.zmanim(
            latitude = lat,
            longitude = lng,
            date = date.toString(),
            tzid = tzid,
        )
        val result = parseZmanim(response.times.orEmpty())
        zmanimCache.put(key, result)
        return result
    }

    suspend fun dayInfo(date: LocalDate = LocalDate.now()): JewishDayInfo {
        val key = date.toString()
        dayInfoCache.get(key)?.let { return it }

        val response = api.calendar(start = key, end = key)
        val result = parseDayInfo(response.items.orEmpty())
        dayInfoCache.put(key, result)
        return result
    }

    private fun parseZmanim(times: Map<String, String>): List<HalachicTime> {
        return ZMANIM_ORDER.mapNotNull { spec ->
            val raw = times[spec.key] ?: return@mapNotNull null
            HalachicTime(
                key = spec.key,
                label = spec.label,
                time = formatTime(raw),
                group = spec.group,
            )
        }
    }

    private fun parseDayInfo(items: List<CalendarItem>): JewishDayInfo {
        var dafYomi: String? = null
        var dafLink: String? = null
        val specials = mutableListOf<SpecialDay>()

        for (item in items) {
            when (item.category) {
                "dafyomi" -> {
                    dafYomi = item.hebrew?.takeIf { it.isNotBlank() } ?: item.title
                    dafLink = item.link
                }
                "hebdate" -> Unit
                else -> {
                    val title = item.hebrew?.takeIf { it.isNotBlank() } ?: item.title ?: continue
                    specials += SpecialDay(
                        title = title,
                        category = item.category.orEmpty(),
                        memo = item.memo,
                        link = item.link,
                    )
                }
            }
        }
        return JewishDayInfo(dafYomi = dafYomi, dafYomiLink = dafLink, specialDays = specials)
    }

    private fun formatTime(iso: String): String = runCatching {
        OffsetDateTime.parse(iso).toLocalTime().format(TIME_FORMAT)
    }.getOrDefault(iso)

    private fun zmanimKey(lat: Double, lng: Double, date: LocalDate): String {
        // עיגול קל (~100 מ׳) כדי לא לבזבז cache על שינויי GPS קטנים
        val rLat = (lat * 1000).roundToInt()
        val rLng = (lng * 1000).roundToInt()
        return "$date|$rLat|$rLng|${TimeZone.getDefault().id}"
    }

    private data class ZmanSpec(val key: String, val label: String, val group: ZmanimGroup)

    companion object {
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

        private val ZMANIM_ORDER = listOf(
            ZmanSpec("alotHaShachar", "עלות השחר", ZmanimGroup.MORNING),
            ZmanSpec("misheyakir", "משיכיר", ZmanimGroup.MORNING),
            ZmanSpec("sunrise", "הנץ החמה", ZmanimGroup.MORNING),
            ZmanSpec("sofZmanShma", "סוף זמן קריאת שמע (גר״א)", ZmanimGroup.MORNING),
            ZmanSpec("sofZmanShmaMGA", "סוף זמן קריאת שמע (מג״א)", ZmanimGroup.MORNING),
            ZmanSpec("sofZmanTfilla", "סוף זמן תפילה (גר״א)", ZmanimGroup.MORNING),
            ZmanSpec("sofZmanTfillaMGA", "סוף זמן תפילה (מג״א)", ZmanimGroup.MORNING),
            ZmanSpec("chatzot", "חצות היום", ZmanimGroup.DAY),
            ZmanSpec("minchaGedola", "מנחה גדולה", ZmanimGroup.DAY),
            ZmanSpec("minchaKetana", "מנחה קטנה", ZmanimGroup.DAY),
            ZmanSpec("plagHaMincha", "פלג המנחה", ZmanimGroup.DAY),
            ZmanSpec("sunset", "שקיעה", ZmanimGroup.EVENING),
            ZmanSpec("tzeit42min", "צאת הכוכבים (42 דק׳)", ZmanimGroup.EVENING),
            ZmanSpec("tzeit72min", "צאת ר״ת (72 דק׳)", ZmanimGroup.EVENING),
        )

        /** זמן הבא שעדיין לא עבר - לסימון במסך */
        fun nextZman(zmanim: List<HalachicTime>): String? {
            val now = LocalTime.now()
            return zmanim.firstOrNull { zman ->
                runCatching { LocalTime.parse(zman.time, TIME_FORMAT).isAfter(now) }.getOrDefault(false)
            }?.key
        }
    }
}
