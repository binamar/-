package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppLanguage
import com.example.domain.model.DarkContrastMode
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flowday_preferences")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DARK_CONTRAST = stringPreferencesKey("dark_contrast")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val MINIMALIST_MODE = booleanPreferencesKey("minimalist_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val GROUPED_ROUTINE_REMINDERS = booleanPreferencesKey("grouped_routine_reminders")
        val SMART_SNOOZE_MINUTES = intPreferencesKey("smart_snooze_minutes")
        val REMINDER_GROUP_WINDOW_MINUTES = intPreferencesKey("reminder_group_window_minutes")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            } ?: ThemeMode.SYSTEM,
            darkContrastMode = prefs[Keys.DARK_CONTRAST]?.let {
                runCatching { DarkContrastMode.valueOf(it) }.getOrNull()
            } ?: DarkContrastMode.BALANCED,
            fontScale = (prefs[Keys.FONT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.30f),
            minimalistMode = prefs[Keys.MINIMALIST_MODE] ?: false,
            language = prefs[Keys.LANGUAGE]?.let {
                runCatching { AppLanguage.valueOf(it) }.getOrNull()
            } ?: AppLanguage.SYSTEM,
            groupedRoutineReminders = prefs[Keys.GROUPED_ROUTINE_REMINDERS] ?: true,
            smartSnoozeMinutes = prefs[Keys.SMART_SNOOZE_MINUTES] ?: 15,
            reminderGroupWindowMinutes = prefs[Keys.REMINDER_GROUP_WINDOW_MINUTES] ?: 30
        )
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updateDarkContrast(contrast: DarkContrastMode) {
        context.dataStore.edit { it[Keys.DARK_CONTRAST] = contrast.name }
    }

    suspend fun updateFontScale(scale: Float) {
        context.dataStore.edit { it[Keys.FONT_SCALE] = scale.coerceIn(0.85f, 1.30f) }
    }

    suspend fun updateMinimalistMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MINIMALIST_MODE] = enabled }
    }

    suspend fun updateLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun updateGroupedRoutineReminders(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GROUPED_ROUTINE_REMINDERS] = enabled }
    }

    suspend fun updateSmartSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.SMART_SNOOZE_MINUTES] = minutes.coerceIn(5, 120) }
    }
}
