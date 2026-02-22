package com.example.shoptools.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoptools.feature.settings.data.AppSettings
import com.example.shoptools.feature.settings.data.FontSizePreset
import com.example.shoptools.feature.settings.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val fontSizePreset: FontSizePreset = FontSizePreset.NORMAL,
    val useDigitSeparator: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = repository.settingsFlow
        .map { settings ->
            SettingsUiState(
                fontSizePreset = settings.fontSizePreset,
                useDigitSeparator = settings.useDigitSeparator,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun setFontSizePreset(preset: FontSizePreset) {
        viewModelScope.launch { repository.setFontSizePreset(preset) }
    }

    fun setUseDigitSeparator(enabled: Boolean) {
        viewModelScope.launch { repository.setUseDigitSeparator(enabled) }
    }
}
