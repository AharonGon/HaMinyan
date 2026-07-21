package com.haminyan.app.util

import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object ErrorInfo {

    /** הסבר ידידותי למשתמש על סיבת השגיאה */
    fun friendly(e: Throwable): String = when (e) {
        is HttpException -> when (e.code()) {
            403 -> "השרת חסם את הגישה (403). ייתכן שהכתובת שלכם נחסמה זמנית - נסו רשת אחרת או המתינו מספר שעות."
            404 -> "הכתובת לא נמצאה בשרת (404)."
            429 -> "יותר מדי בקשות (429). המתינו מעט ונסו שוב."
            in 500..599 -> "תקלה בשרת נדרים פלוס (${e.code()}). נסו שוב מאוחר יותר."
            else -> "השרת החזיר שגיאה ${e.code()}."
        }
        is UnknownHostException -> "אין חיבור לאינטרנט, או שהשרת אינו זמין."
        is SocketTimeoutException -> "השרת לא הגיב בזמן. בדקו את החיבור ונסו שוב."
        is SSLException -> "שגיאת אבטחה בחיבור לשרת."
        else -> "שגיאה לא צפויה: ${e.message ?: e.javaClass.simpleName}"
    }

    /** פרטים טכניים מלאים לצורך דיווח תקלה */
    fun technical(e: Throwable, context: String): String = buildString {
        appendLine("Context: $context")
        appendLine("Type: ${e.javaClass.name}")
        if (e is HttpException) {
            appendLine("HTTP: ${e.code()} ${e.message()}")
            appendLine("URL: ${e.response()?.raw()?.request?.url}")
        }
        appendLine("Message: ${e.message}")
        e.cause?.let { appendLine("Cause: ${it.javaClass.simpleName}: ${it.message}") }
    }.trim()
}
