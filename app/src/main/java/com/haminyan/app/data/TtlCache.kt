package com.haminyan.app.data

import android.os.SystemClock

/**
 * Cache פשוט בזיכרון עם תוקף זמן (TTL), להקטנת בקשות רשת חוזרות.
 * משתמש ב-elapsedRealtime כדי לא להיות מושפע משינויי שעון.
 */
class TtlCache<K, V>(private val ttlMillis: Long) {

    private data class Entry<V>(val value: V, val expiresAt: Long)

    private val map = HashMap<K, Entry<V>>()

    @Synchronized
    fun get(key: K): V? {
        val entry = map[key] ?: return null
        if (SystemClock.elapsedRealtime() > entry.expiresAt) {
            map.remove(key)
            return null
        }
        return entry.value
    }

    @Synchronized
    fun put(key: K, value: V) {
        map[key] = Entry(value, SystemClock.elapsedRealtime() + ttlMillis)
    }
}
