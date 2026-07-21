package com.haminyan.app.data.model

import com.google.gson.annotations.SerializedName

/** תוצאת חיפוש בית כנסת לפי שם (GetClientBox) */
data class ClientBoxResponse(
    @SerializedName("ClientBox") val clientBox: List<MosadResult>? = null,
)

data class MosadResult(
    @SerializedName("MosadId") val mosadId: String = "",
    @SerializedName("MosadName") val mosadName: String = "",
)

/** לוח זמנים של מוסד בודד (GetMosadMinyan) */
data class MosadMinyanResponse(
    @SerializedName("Day") val day: String? = null,
    @SerializedName("data") val data: List<MinyanItem>? = null,
    @SerializedName("Message") val message: String? = null,
)

data class MinyanItem(
    @SerializedName("T") val type: String? = null,      // סוג תפילה
    @SerializedName("L") val room: String? = null,      // מיקום בתוך המוסד
    @SerializedName("H") val time: String? = null,      // שעה HH:mm
    @SerializedName("D") val days: String? = null,      // אותיות ימים אבגדהוש
    @SerializedName("C") val comment: String? = null,   // הערה
)

/** מניינים קרובים לפי מיקום (GetNearestMinyan) */
data class NearestResponse(
    @SerializedName("data") val data: List<NearbyMinyan>? = null,
)

data class NearbyMinyan(
    @SerializedName("T") val type: String? = null,      // סוג תפילה
    @SerializedName("L") val name: String? = null,      // שם בית הכנסת
    @SerializedName("H") val time: String? = null,      // שעה HH:mm
    @SerializedName("C") val comment: String? = null,   // הערה
    @SerializedName("D") val distance: String? = null,  // מרחק אווירי טקסטואלי (מנדרים פלוס)
    @SerializedName("M") val mosadId: String? = null,
    @SerializedName("A") val address: String? = null,
    @SerializedName("W") val coords: String? = null,    // "lat*lng"
    // שדות מחושבים מקומית - מרחק וזמן הליכה אמיתיים (OpenRouteService). לא מגיעים מה-JSON.
    @Transient val walkMeters: Double? = null,
    @Transient val walkSeconds: Double? = null,
) {
    val lat: Double? get() = coords?.substringBefore('*')?.toDoubleOrNull()
    val lng: Double? get() = coords?.substringAfter('*')?.toDoubleOrNull()

    val hasCoords: Boolean get() = lat != null && lng != null

    /** מרחק אפקטיבי במטרים למיון: העדפה למרחק הליכה, אחרת פענוח המרחק האווירי */
    val effectiveMeters: Double
        get() = walkMeters ?: parseAerialMeters(distance) ?: Double.MAX_VALUE

    companion object {
        /** פענוח "313 מטר" / "1.8 ק\"מ" למטרים */
        fun parseAerialMeters(text: String?): Double? {
            if (text.isNullOrBlank()) return null
            val number = Regex("""[\d.]+""").find(text)?.value?.toDoubleOrNull() ?: return null
            return if (text.contains("ק")) number * 1000.0 else number
        }
    }
}

/** בית כנסת שמור במועדפים */
data class FavoriteMosad(
    val id: String,
    val name: String,
)
