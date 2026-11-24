package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AppLanguage
import com.example.domain.model.DarkContrastMode
import com.example.domain.model.ThemeMode
import com.example.domain.model.UserPreferences
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

    val preferences: StateFlow<UserPreferences> = settingsRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.updateThemeMode(mode) }
    }

    fun setDarkContrast(contrast: DarkContrastMode) {
        viewModelScope.launch { settingsRepository.updateDarkContrast(contrast) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { settingsRepository.updateFontScale(scale) }
    }

    fun setMinimalistMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateMinimalistMode(enabled) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.updateLanguage(language) }
    }

    fun setGroupedRoutineReminders(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateGroupedRoutineReminders(enabled) }
    }

    fun setSmartSnoozeMinutes(minutes: Int) {
        viewModelScope.launch { settingsRepository.updateSmartSnoozeMinutes(minutes) }
    }
}
