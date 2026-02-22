package com.example.shoptools.feature.unitprice.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.shoptools.core.formatUnitPrice
import com.example.shoptools.design.ErrorText
import com.example.shoptools.design.LargeResultCard
import com.example.shoptools.feature.unitprice.ProductRow
import com.example.shoptools.feature.unitprice.UnitPriceEvent
import com.example.shoptools.feature.unitprice.UnitPriceResult
import com.example.shoptools.feature.unitprice.UnitPriceViewModel

@Composable
fun UnitPriceScreen(viewModel: UnitPriceViewModel) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // --- Result section ---
        if (state.results.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.empty_unit_price),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            val cheapest = state.results.first()
            item {
                LargeResultCard(
                    title = stringResource(R.string.label_cheapest) + (if (cheapest.name.isNotBlank()) "  ${cheapest.name}" else ""),
                    mainValue = "${formatUnitPrice(cheapest.unitPrice, state.useDigitSeparator)} ${stringResource(R.string.label_unit_price)}",
                    subInfo = buildSubInfo(cheapest),
                    highlighted = true,
                )
            }
            // Other results (index offset by 1 because cheapest is shown separately above)
            if (state.results.size > 1) {
                itemsIndexed(state.results.drop(1)) { index, result ->
                    LargeResultCard(
                        title = if (result.name.isNotBlank()) result.name else "商品 ${index + 2}",
                        mainValue = "${formatUnitPrice(result.unitPrice, state.useDigitSeparator)} ${stringResource(R.string.label_unit_price)}",
                        subInfo = buildSubInfo(result),
                        highlighted = false,
                    )
                }
            }
        }

        // --- Row editors ---
        items(state.rows, key = { it.id }) { row ->
            ProductRowEditor(
                row = row,
                onUpdate = { viewModel.onEvent(UnitPriceEvent.UpdateRow(it)) },
                onRemove = { viewModel.onEvent(UnitPriceEvent.RemoveRow(row.id)) },
                canRemove = state.rows.size > 1,
            )
        }

        // --- Add button ---
        item {
            Button(
                onClick = { viewModel.onEvent(UnitPriceEvent.AddRow) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.btn_add))
            }
        }
    }
}

private fun buildSubInfo(result: UnitPriceResult): String {
    val unitStr = if (result.unit.isNotBlank()) result.unit else "unit"
    return "¥${result.price.toInt()} / ${result.totalQuantity}${unitStr}" +
            if (result.count > 1) " (×${result.count})" else ""
}

@Composable
private fun ProductRowEditor(
    row: ProductRow,
    onUpdate: (ProductRow) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = row.name,
                    onValueChange = { onUpdate(row.copy(name = it)) },
                    label = { Text(stringResource(R.string.hint_product_name)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "削除")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = row.price,
                        onValueChange = { onUpdate(row.copy(price = it)) },
                        label = { Text(stringResource(R.string.hint_price)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = row.priceError.isNotBlank(),
                        singleLine = true,
                    )
                    if (row.priceError.isNotBlank()) ErrorText(row.priceError)
                }
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = row.quantity,
                        onValueChange = { onUpdate(row.copy(quantity = it)) },
                        label = { Text(stringResource(R.string.hint_quantity)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = row.quantityError.isNotBlank(),
                        singleLine = true,
                    )
                    if (row.quantityError.isNotBlank()) ErrorText(row.quantityError)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = row.unit,
                    onValueChange = { onUpdate(row.copy(unit = it)) },
                    label = { Text(stringResource(R.string.hint_unit)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = row.count,
                        onValueChange = { onUpdate(row.copy(count = it)) },
                        label = { Text(stringResource(R.string.hint_count)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = row.countError.isNotBlank(),
                        singleLine = true,
                    )
                    if (row.countError.isNotBlank()) ErrorText(row.countError)
                }
            }
        }
    }
}
