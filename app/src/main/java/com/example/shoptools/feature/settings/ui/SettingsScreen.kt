package com.example.shoptools.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.shoptools.R
import com.example.shoptools.feature.settings.SettingsViewModel
import com.example.shoptools.feature.settings.data.FontSizePreset

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // --- Font size ---
        Text(
            text = stringResource(R.string.settings_font_size),
            style = MaterialTheme.typography.titleMedium,
        )
        val fontOptions = listOf(
            FontSizePreset.NORMAL to stringResource(R.string.settings_font_normal),
            FontSizePreset.LARGE to stringResource(R.string.settings_font_large),
            FontSizePreset.XLARGE to stringResource(R.string.settings_font_xlarge),
        )
        Column(modifier = Modifier.selectableGroup()) {
            fontOptions.forEach { (preset, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = state.fontSizePreset == preset,
                            onClick = { viewModel.setFontSizePreset(preset) },
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = state.fontSizePreset == preset,
                        onClick = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = label, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        HorizontalDivider()

        // --- Digit separator ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_digit_separator),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = state.useDigitSeparator,
                onCheckedChange = { viewModel.setUseDigitSeparator(it) },
            )
        }
    }
}
