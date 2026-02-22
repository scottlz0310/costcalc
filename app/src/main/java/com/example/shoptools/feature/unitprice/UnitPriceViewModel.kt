package com.example.shoptools.feature.unitprice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoptools.core.*
import com.example.shoptools.feature.settings.data.SettingsRepository
import com.example.shoptools.feature.unitprice.domain.UnitPriceCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

data class ProductRow(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val price: String = "",
    val quantity: String = "",
    val unit: String = "",
    val count: String = "1",
    val priceError: String = "",
    val quantityError: String = "",
    val countError: String = "",
)

data class UnitPriceResult(
    val id: String,
    val name: String,
    val unitPrice: Double,
    val totalQuantity: Double,
    val unit: String,
    val price: Double,
    val count: Int,
)

data class UnitPriceUiState(
    val rows: List<ProductRow> = emptyList(),
    val results: List<UnitPriceResult> = emptyList(),
    val useDigitSeparator: Boolean = false,
)

sealed class UnitPriceEvent {
    object AddRow : UnitPriceEvent()
    data class UpdateRow(val row: ProductRow) : UnitPriceEvent()
    data class RemoveRow(val id: String) : UnitPriceEvent()
}

@HiltViewModel
class UnitPriceViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnitPriceUiState(rows = listOf(ProductRow(), ProductRow())))
    val uiState: StateFlow<UnitPriceUiState> = _uiState.asStateFlow()

    init {
        settingsRepository.settingsFlow
            .onEach { settings ->
                _uiState.update { it.copy(useDigitSeparator = settings.useDigitSeparator) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: UnitPriceEvent) {
        when (event) {
            is UnitPriceEvent.AddRow -> addRow()
            is UnitPriceEvent.UpdateRow -> updateRow(event.row)
            is UnitPriceEvent.RemoveRow -> removeRow(event.id)
        }
    }

    private fun addRow() {
        _uiState.update { state ->
            state.copy(rows = state.rows + ProductRow())
        }
        recalculate()
    }

    private fun updateRow(updated: ProductRow) {
        _uiState.update { state ->
            val validated = validated(updated)
            state.copy(rows = state.rows.map { if (it.id == updated.id) validated else it })
        }
        recalculate()
    }

    private fun removeRow(id: String) {
        _uiState.update { state ->
            state.copy(rows = state.rows.filter { it.id != id })
        }
        recalculate()
    }

    private fun validated(row: ProductRow): ProductRow {
        val priceErr = if (row.price.isBlank()) "" else validatePrice(row.price).let {
            if (!it.isValid) it.errorMessage else ""
        }
        val quantityErr = if (row.quantity.isBlank()) "" else validateQuantity(row.quantity).let {
            if (!it.isValid) it.errorMessage else ""
        }
        val countErr = if (row.count.isBlank()) "" else validateCount(row.count).let {
            if (!it.isValid) it.errorMessage else ""
        }
        return row.copy(priceError = priceErr, quantityError = quantityErr, countError = countErr)
    }

    private fun recalculate() {
        val rows = _uiState.value.rows
        val results = rows.mapNotNull { row ->
            val price = row.price.toDoubleOrNull() ?: return@mapNotNull null
            val quantity = row.quantity.toDoubleOrNull() ?: return@mapNotNull null
            val count = row.count.toIntOrNull() ?: 1
            if (price <= 0 || quantity <= 0 || count < 1) return@mapNotNull null
            val unitPrice = UnitPriceCalculator.calculate(price, quantity, count)
            UnitPriceResult(
                id = row.id,
                name = row.name,
                unitPrice = unitPrice,
                totalQuantity = quantity * count,
                unit = row.unit,
                price = price,
                count = count,
            )
        }.sortedBy { it.unitPrice }
        _uiState.update { it.copy(results = results) }
    }
}
