package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.QualityPreferencePolicy
import com.example.domain.model.ScraperSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "streamgrab_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val QUALITY_POLICY = stringPreferencesKey("quality_policy")
        val MAX_CONCURRENT = intPreferencesKey("max_concurrent_downloads")
        val INTER_DELAY_SEC = intPreferencesKey("inter_download_delay_sec")
        val AUTO_RETRY_COUNT = intPreferencesKey("auto_retry_count")
        val PREFER_SUBTITLES = booleanPreferencesKey("prefer_mixdrop_subtitles")
        val USER_AGENT = stringPreferencesKey("custom_user_agent")
        val DARK_THEME = booleanPreferencesKey("use_dark_theme")
    }

    val settings: Flow<ScraperSettings> = context.settingsDataStore.data.map { prefs ->
        val policyName = prefs[Keys.QUALITY_POLICY]
        val policy = QualityPreferencePolicy.entries.firstOrNull { it.name == policyName }
            ?: QualityPreferencePolicy.TARGET_360P_SMART_FALLBACK
        ScraperSettings(
            qualityPolicy = policy,
            maxConcurrentDownloads = (prefs[Keys.MAX_CONCURRENT] ?: 2).coerceIn(1, 3),
            interDownloadDelaySec = (prefs[Keys.INTER_DELAY_SEC] ?: 3).coerceIn(0, 30),
            autoRetryCount = (prefs[Keys.AUTO_RETRY_COUNT] ?: 2).coerceIn(0, 5),
            preferMixDropSubtitles = prefs[Keys.PREFER_SUBTITLES] ?: true,
            customUserAgent = prefs[Keys.USER_AGENT] ?: ScraperSettings.DEFAULT_DESKTOP_USER_AGENT,
            useDarkTheme = prefs[Keys.DARK_THEME] ?: true
        )
    }

    suspend fun updateQualityPolicy(policy: QualityPreferencePolicy) {
        context.settingsDataStore.edit { it[Keys.QUALITY_POLICY] = policy.name }
    }

    suspend fun updateMaxConcurrent(count: Int) {
        context.settingsDataStore.edit { it[Keys.MAX_CONCURRENT] = count.coerceIn(1, 3) }
    }

    suspend fun updateInterDelay(seconds: Int) {
        context.settingsDataStore.edit { it[Keys.INTER_DELAY_SEC] = seconds.coerceIn(0, 30) }
    }

    suspend fun updateAutoRetry(count: Int) {
        context.settingsDataStore.edit { it[Keys.AUTO_RETRY_COUNT] = count.coerceIn(0, 5) }
    }

    suspend fun updatePreferSubtitles(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.PREFER_SUBTITLES] = enabled }
    }

    suspend fun updateUserAgent(userAgent: String) {
        context.settingsDataStore.edit { it[Keys.USER_AGENT] = userAgent }
    }

    suspend fun updateDarkTheme(dark: Boolean) {
        context.settingsDataStore.edit { it[Keys.DARK_THEME] = dark }
    }
}
