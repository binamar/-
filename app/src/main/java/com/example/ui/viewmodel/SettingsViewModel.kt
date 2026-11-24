package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.QualityPreferencePolicy
import com.example.domain.model.ScraperSettings
import com.example.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<ScraperSettings> = settingsRepository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ScraperSettings()
        )

    fun updateQualityPolicy(policy: QualityPreferencePolicy) = viewModelScope.launch {
        settingsRepository.updateQualityPolicy(policy)
    }

    fun updateMaxConcurrent(count: Int) = viewModelScope.launch {
        settingsRepository.updateMaxConcurrent(count)
    }

    fun updateInterDelay(seconds: Int) = viewModelScope.launch {
        settingsRepository.updateInterDownloadDelay(seconds)
    }

    fun updateAutoRetry(count: Int) = viewModelScope.launch {
        settingsRepository.updateAutoRetryCount(count)
    }

    fun updatePreferSubtitles(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.updatePreferSubtitles(enabled)
    }

    fun updateUserAgent(ua: String) = viewModelScope.launch {
        settingsRepository.updateUserAgent(ua)
    }

    fun updateDarkTheme(dark: Boolean) = viewModelScope.launch {
        settingsRepository.updateDarkTheme(dark)
    }
}
