package com.dhyey.fanfic.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "reader_settings")

enum class ReaderTheme(val displayName: String, val bgColor: Long, val textColor: Long) {
    DARK("Dark", 0xFF1A1A1A, 0xFFE0E0E0),
    LIGHT("Light", 0xFFFFFBF5, 0xFF2C2C2C),
    SEPIA("Sepia", 0xFFF5E6C8, 0xFF5B4636),
    AMOLED("AMOLED", 0xFF000000, 0xFFFFFFFF),
    FOREST("Forest", 0xFF1A2F1A, 0xFFD4E8D4),
    OCEAN("Ocean", 0xFF1A1A2F, 0xFFD4D4E8)
}

data class ReaderSettings(
    val theme: ReaderTheme = ReaderTheme.DARK,
    val fontSize: Float = 18f,
    val lineHeight: Float = 1.7f
)

@Singleton
class ReaderPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeKey = intPreferencesKey("reader_theme")
    private val fontSizeKey = floatPreferencesKey("font_size")
    private val lineHeightKey = floatPreferencesKey("line_height")

    val settings: Flow<ReaderSettings> = context.dataStore.data.map { prefs ->
        ReaderSettings(
            theme = ReaderTheme.entries.getOrElse(prefs[themeKey] ?: 0) { ReaderTheme.DARK },
            fontSize = prefs[fontSizeKey] ?: 18f,
            lineHeight = prefs[lineHeightKey] ?: 1.7f
        )
    }

    suspend fun setTheme(theme: ReaderTheme) {
        context.dataStore.edit { prefs ->
            prefs[themeKey] = theme.ordinal
        }
    }

    suspend fun setFontSize(size: Float) {
        context.dataStore.edit { prefs ->
            prefs[fontSizeKey] = size.coerceIn(12f, 32f)
        }
    }

    suspend fun setLineHeight(height: Float) {
        context.dataStore.edit { prefs ->
            prefs[lineHeightKey] = height.coerceIn(1.2f, 2.5f)
        }
    }
}
