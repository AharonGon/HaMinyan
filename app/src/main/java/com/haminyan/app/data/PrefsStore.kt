package com.haminyan.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.haminyan.app.data.model.FavoriteMosad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "haminyan_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class PrefsStore(private val context: Context) {

    private val gson = Gson()

    private object Keys {
        val FAVORITES = stringPreferencesKey("favorites")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        val RADIUS_KM = intPreferencesKey("radius_km")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_PAST = booleanPreferencesKey("show_past")
    }

    val favorites: Flow<List<FavoriteMosad>> = context.dataStore.data.map { prefs ->
        parseFavorites(prefs[Keys.FAVORITES])
    }

    val recentSearches: Flow<List<String>> = context.dataStore.data.map { prefs ->
        parseStrings(prefs[Keys.RECENT_SEARCHES])
    }

    val radiusKm: Flow<Int> = context.dataStore.data.map { it[Keys.RADIUS_KM] ?: 2 }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map {
        runCatching { ThemeMode.valueOf(it[Keys.THEME_MODE] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun toggleFavorite(mosad: FavoriteMosad) {
        context.dataStore.edit { prefs ->
            val current = parseFavorites(prefs[Keys.FAVORITES]).toMutableList()
            val existing = current.indexOfFirst { it.id == mosad.id }
            if (existing >= 0) current.removeAt(existing) else current.add(0, mosad)
            prefs[Keys.FAVORITES] = gson.toJson(current)
        }
    }

    suspend fun addRecentSearch(query: String) {
        if (query.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = parseStrings(prefs[Keys.RECENT_SEARCHES]).toMutableList()
            current.remove(query)
            current.add(0, query)
            prefs[Keys.RECENT_SEARCHES] = gson.toJson(current.take(8))
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { it[Keys.RECENT_SEARCHES] = "[]" }
    }

    suspend fun setRadiusKm(value: Int) {
        context.dataStore.edit { it[Keys.RADIUS_KM] = value }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    private fun parseFavorites(json: String?): List<FavoriteMosad> {
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<FavoriteMosad>>() {}.type
        return runCatching { gson.fromJson<List<FavoriteMosad>>(json, type) }.getOrNull().orEmpty()
    }

    private fun parseStrings(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return runCatching { gson.fromJson<List<String>>(json, type) }.getOrNull().orEmpty()
    }
}
