package com.example.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppBackgroundTheme(
    val displayNameAr: String,
    val previewSwatchHex: Long
) {
    SKY_BLUE("أزرق سماوي هادئ", 0xFFEAF3FB),
    EMERALD_CREAM("أخضر زمردي وكريمي", 0xFFFAF6EE),
    SAND_GOLD("رملي ذهبي دافئ", 0xFFFBF3E4),
    WHITE_BLACK("أبيض وأسود", 0xFFFFFFFF),
    DARK_NIGHT("أسود ليلي داكن", 0xFF0B0B0D)
}

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "sirah_theme_prefs")

object ThemePreferences {
    private val KEY_THEME = stringPreferencesKey("selected_background_theme")

    fun observeTheme(context: Context): Flow<AppBackgroundTheme> {
        return context.applicationContext.themeDataStore.data.map { prefs ->
            val raw = prefs[KEY_THEME]
            AppBackgroundTheme.entries.firstOrNull { it.name == raw } ?: AppBackgroundTheme.SKY_BLUE
        }
    }

    suspend fun saveTheme(context: Context, theme: AppBackgroundTheme) {
        context.applicationContext.themeDataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }
}
