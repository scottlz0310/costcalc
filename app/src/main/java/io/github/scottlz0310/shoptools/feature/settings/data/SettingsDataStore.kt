package io.github.scottlz0310.shoptools.feature.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class FontSizePreset { NORMAL, LARGE, XLARGE }

data class AppSettings(
    val fontSizePreset: FontSizePreset = FontSizePreset.NORMAL,
    val useDigitSeparator: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val fontSizePresetKey = stringPreferencesKey("font_size_preset")
        private val useDigitSeparatorKey = booleanPreferencesKey("use_digit_separator")

        val settingsFlow: Flow<AppSettings> =
            context.dataStore.data.map { prefs ->
                AppSettings(
                    fontSizePreset =
                        prefs[fontSizePresetKey]?.let {
                            runCatching { FontSizePreset.valueOf(it) }.getOrNull()
                        } ?: FontSizePreset.NORMAL,
                    useDigitSeparator = prefs[useDigitSeparatorKey] ?: false,
                )
            }

        suspend fun setFontSizePreset(preset: FontSizePreset) {
            context.dataStore.edit { prefs ->
                prefs[fontSizePresetKey] = preset.name
            }
        }

        suspend fun setUseDigitSeparator(enabled: Boolean) {
            context.dataStore.edit { prefs ->
                prefs[useDigitSeparatorKey] = enabled
            }
        }
    }
