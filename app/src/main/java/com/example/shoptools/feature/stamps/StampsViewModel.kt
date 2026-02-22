package com.example.shoptools.feature.stamps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoptools.core.*
import com.example.shoptools.feature.settings.data.SettingsRepository
import com.example.shoptools.feature.stamps.data.StampsRepository
import com.example.shoptools.feature.stamps.domain.BoundedSubsetSum
import com.example.shoptools.feature.stamps.domain.StampCombination
import com.example.shoptools.feature.stamps.domain.StampItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class StampInventoryRow(
    val id: String = UUID.randomUUID().toString(),
    val denomination: String = "",
    val stock: String = "",
    val denominationError: String = "",
    val stockError: String = "",
)

data class StampsUiState(
    val rows: List<StampInventoryRow> = emptyList(),
    val target: String = "",
    val targetError: String = "",
    val exact: StampCombination? = null,
    val under: List<StampCombination> = emptyList(),
    val over: List<StampCombination> = emptyList(),
    val hasResult: Boolean = false,
    val useDigitSeparator: Boolean = false,
)

sealed class StampsEvent {
    object AddRow : StampsEvent()
    data class UpdateDenomination(val id: String, val value: String) : StampsEvent()
    data class UpdateStock(val id: String, val value: String) : StampsEvent()
    data class RemoveRow(val id: String) : StampsEvent()
    data class ClearRow(val id: String) : StampsEvent()
    data class UpdateTarget(val value: String) : StampsEvent()
    object Calculate : StampsEvent()
}

@HiltViewModel
class StampsViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val stampsRepository: StampsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        StampsUiState(rows = listOf(StampInventoryRow(), StampInventoryRow()))
    )
    val uiState: StateFlow<StampsUiState> = _uiState.asStateFlow()

    init {
        settingsRepository.settingsFlow
            .onEach { settings ->
                _uiState.update { it.copy(useDigitSeparator = settings.useDigitSeparator) }
            }
            .launchIn(viewModelScope)

        // 保存済みデータを初期ロード
        viewModelScope.launch {
            val saved = stampsRepository.stampsFlow.first()
            if (saved.rows.isNotEmpty()) {
                _uiState.update { state ->
                    state.copy(
                        rows = saved.rows.map { (denom, stock) ->
                            StampInventoryRow(denomination = denom, stock = stock)
                        },
                        target = saved.target,
                    )
                }
            }
        }
    }

    fun onEvent(event: StampsEvent) {
        when (event) {
            is StampsEvent.AddRow -> addRow()
            is StampsEvent.UpdateDenomination -> updateDenomination(event.id, event.value)
            is StampsEvent.UpdateStock -> updateStock(event.id, event.value)
            is StampsEvent.RemoveRow -> removeRow(event.id)
            is StampsEvent.ClearRow -> clearRow(event.id)
            is StampsEvent.UpdateTarget -> updateTarget(event.value)
            is StampsEvent.Calculate -> calculate()
        }
    }

    private fun addRow() {
        _uiState.update { it.copy(rows = it.rows + StampInventoryRow()) }
        saveRows()
    }

    private fun updateDenomination(id: String, value: String) {
        _uiState.update { state ->
            state.copy(
                rows = state.rows.map { row ->
                    if (row.id != id) row else {
                        val err = if (value.isBlank()) "" else validateDenomination(value).let {
                            if (!it.isValid) it.errorMessage else ""
                        }
                        row.copy(denomination = value, denominationError = err)
                    }
                },
                hasResult = false,
            )
        }
        saveRows()
    }

    private fun updateStock(id: String, value: String) {
        _uiState.update { state ->
            state.copy(
                rows = state.rows.map { row ->
                    if (row.id != id) row else {
                        val err = if (value.isBlank()) "" else validateStock(value).let {
                            if (!it.isValid) it.errorMessage else ""
                        }
                        row.copy(stock = value, stockError = err)
                    }
                },
                hasResult = false,
            )
        }
        saveRows()
    }

    private fun removeRow(id: String) {
        _uiState.update { s -> s.copy(rows = s.rows.filter { it.id != id }) }
        saveRows()
    }

    private fun clearRow(id: String) {
        _uiState.update { state ->
            state.copy(
                rows = state.rows.map { row ->
                    if (row.id != id) row
                    else row.copy(denomination = "", stock = "", denominationError = "", stockError = "")
                },
                hasResult = false,
            )
        }
        saveRows()
    }

    private fun updateTarget(value: String) {
        _uiState.update { it.copy(target = value, targetError = "", hasResult = false) }
        viewModelScope.launch { stampsRepository.saveTarget(value) }
    }

    private fun saveRows() {
        viewModelScope.launch {
            val rows = _uiState.value.rows.map { it.denomination to it.stock }
            stampsRepository.saveRows(rows)
        }
    }

    private fun calculate() {
        val state = _uiState.value

        // Validate target
        val targetValidation = validateTarget(state.target)
        if (!targetValidation.isValid) {
            _uiState.update { it.copy(targetError = targetValidation.errorMessage) }
            return
        }

        // Validate all rows
        val hasRowErrors = state.rows.any { row ->
            row.denomination.isNotBlank() && validateDenomination(row.denomination).let { !it.isValid } ||
                    row.stock.isNotBlank() && validateStock(row.stock).let { !it.isValid }
        }
        if (hasRowErrors) return

        val inventory = state.rows.mapNotNull { row ->
            val denom = row.denomination.toIntOrNull() ?: return@mapNotNull null
            val stock = row.stock.toIntOrNull() ?: return@mapNotNull null
            if (denom <= 0 || stock <= 0) return@mapNotNull null
            StampItem(denom, stock)
        }

        val target = state.target.toInt()
        val (exact, under, over) = BoundedSubsetSum.solve(inventory, target)
        _uiState.update { it.copy(exact = exact, under = under, over = over, hasResult = true, targetError = "") }
    }
}
