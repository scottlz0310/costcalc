package com.example.shoptools.feature.stamps.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.stampsDataStore: DataStore<Preferences> by preferencesDataStore(name = "stamps")

data class PersistedStampsData(
    val rows: List<Pair<String, String>>,  // denomination to stock
    val target: String,
)

@Singleton
class StampsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val ROWS_KEY = stringPreferencesKey("stamps_rows")
    private val TARGET_KEY = stringPreferencesKey("stamps_target")

    val stampsFlow: Flow<PersistedStampsData> = context.stampsDataStore.data.map { prefs ->
        PersistedStampsData(
            rows = decodeRows(prefs[ROWS_KEY] ?: ""),
            target = prefs[TARGET_KEY] ?: "",
        )
    }

    suspend fun saveState(rows: List<Pair<String, String>>, target: String) {
        context.stampsDataStore.edit { prefs ->
            prefs[ROWS_KEY] = encodeRows(rows)
            prefs[TARGET_KEY] = target
        }
    }

    private fun encodeRows(rows: List<Pair<String, String>>): String =
        rows.joinToString("|") { "${it.first}:${it.second}" }

    private fun decodeRows(encoded: String): List<Pair<String, String>> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split("|").mapNotNull { pair ->
            val parts = pair.split(":", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }
    }
}
