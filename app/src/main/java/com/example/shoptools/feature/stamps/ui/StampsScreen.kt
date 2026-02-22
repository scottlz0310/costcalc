package com.example.shoptools.feature.stamps.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.shoptools.R
import com.example.shoptools.core.formatAmount
import com.example.shoptools.design.ErrorText
import com.example.shoptools.design.LargeResultCard
import com.example.shoptools.feature.stamps.StampInventoryRow
import com.example.shoptools.feature.stamps.StampsEvent
import com.example.shoptools.feature.stamps.StampsViewModel
import com.example.shoptools.feature.stamps.domain.BoundedSubsetSum
import com.example.shoptools.feature.stamps.domain.StampCombination

@Composable
fun StampsScreen(viewModel: StampsViewModel) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // --- Result section ---
        if (!state.hasResult) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.empty_stamps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            if (state.exact != null) {
                item {
                    LargeResultCard(
                        title = stringResource(R.string.label_exact),
                        mainValue = "${formatAmount(state.exact!!.total, state.useDigitSeparator)}円",
                        subInfo = "${state.exact!!.pieces}${stringResource(R.string.label_pieces)}  ${BoundedSubsetSum.compositionToString(state.exact!!.composition)}",
                        highlighted = true,
                    )
                }
            }
            if (state.under.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.label_under),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                items(state.under) { combo ->
                    StampResultCard(combo = combo, target = state.target.toIntOrNull() ?: 0, useDigitSeparator = state.useDigitSeparator)
                }
            }
            if (state.over.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.label_over),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                items(state.over) { combo ->
                    StampResultCard(combo = combo, target = state.target.toIntOrNull() ?: 0, useDigitSeparator = state.useDigitSeparator)
                }
            }
        }

        // --- Target input ---
        item {
            Column {
                OutlinedTextField(
                    value = state.target,
                    onValueChange = { viewModel.onEvent(StampsEvent.UpdateTarget(it)) },
                    label = { Text(stringResource(R.string.hint_target)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = state.targetError.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                if (state.targetError.isNotBlank()) ErrorText(state.targetError)
            }
        }

        // --- Inventory rows ---
        items(state.rows, key = { it.id }) { row ->
            StampRowEditor(
                row = row,
                onDenomChange = { viewModel.onEvent(StampsEvent.UpdateDenomination(row.id, it)) },
                onStockChange = { viewModel.onEvent(StampsEvent.UpdateStock(row.id, it)) },
                onRemove = { viewModel.onEvent(StampsEvent.RemoveRow(row.id)) },
                canRemove = state.rows.size > 1,
            )
        }

        // --- Buttons ---
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.onEvent(StampsEvent.AddRow) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.btn_add))
                }
                Button(
                    onClick = { viewModel.onEvent(StampsEvent.Calculate) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.btn_calculate))
                }
            }
        }
    }
}

@Composable
private fun StampResultCard(combo: StampCombination, target: Int, useDigitSeparator: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "${formatAmount(combo.total, useDigitSeparator)}円",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "差分: ${if (combo.total >= target) "+" else ""}${combo.total - target}円  " +
                        "${combo.pieces}枚  " +
                        BoundedSubsetSum.compositionToString(combo.composition),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StampRowEditor(
    row: StampInventoryRow,
    onDenomChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = row.denomination,
                    onValueChange = onDenomChange,
                    label = { Text(stringResource(R.string.hint_denomination)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = row.denominationError.isNotBlank(),
                    singleLine = true,
                )
                if (row.denominationError.isNotBlank()) ErrorText(row.denominationError)
            }
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = row.stock,
                    onValueChange = onStockChange,
                    label = { Text(stringResource(R.string.hint_stock)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = row.stockError.isNotBlank(),
                    singleLine = true,
                )
                if (row.stockError.isNotBlank()) ErrorText(row.stockError)
            }
            if (canRemove) {
                IconButton(onClick = onRemove, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "削除")
                }
            }
        }
    }
}
