package com.example.expensetracker.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.expensetracker.data.importer.ColumnMapping
import com.example.expensetracker.data.importer.DecimalSeparator
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONObject

val Context.importMappingDataStore by preferencesDataStore(name = "import_mappings")

object ImportMappingStore {
    fun keyForProfile(profileKey: String): Preferences.Key<String> = stringPreferencesKey("mapping_$profileKey")

    suspend fun saveMapping(context: Context, profileKey: String, mapping: ColumnMapping) {
        val json = JSONObject().apply {
            put("dateColumn", mapping.dateColumn)
            mapping.descriptionColumn?.let { put("descriptionColumn", it) }
            mapping.typeColumn?.let { put("typeColumn", it) }
            mapping.amountColumn?.let { put("amountColumn", it) }
            mapping.debitColumn?.let { put("debitColumn", it) }
            mapping.creditColumn?.let { put("creditColumn", it) }
            mapping.dateFormat?.let { put("dateFormat", it) }
            put("decimalSeparator", mapping.decimalSeparator.name)
        }.toString()

        context.importMappingDataStore.edit { prefs ->
            prefs[keyForProfile(profileKey)] = json
        }
    }

    suspend fun loadMapping(context: Context, profileKey: String): ColumnMapping? {
        val prefs = context.importMappingDataStore.data
        val raw = prefs.firstOrNull()?.get(keyForProfile(profileKey)) ?: return null
        return try {
            val json = JSONObject(raw)
            ColumnMapping(
                dateColumn = json.getInt("dateColumn"),
                descriptionColumn = json.optInt("descriptionColumn").takeIf { json.has("descriptionColumn") && !json.isNull("descriptionColumn") },
                typeColumn = json.optInt("typeColumn").takeIf { json.has("typeColumn") && !json.isNull("typeColumn") },
                amountColumn = json.optInt("amountColumn").takeIf { json.has("amountColumn") && !json.isNull("amountColumn") },
                debitColumn = json.optInt("debitColumn").takeIf { json.has("debitColumn") && !json.isNull("debitColumn") },
                creditColumn = json.optInt("creditColumn").takeIf { json.has("creditColumn") && !json.isNull("creditColumn") },
                dateFormat = json.optString("dateFormat").takeIf { it.isNotBlank() },
                decimalSeparator = DecimalSeparator.valueOf(json.optString("decimalSeparator", DecimalSeparator.DOT.name))
            )
        } catch (_: Throwable) {
            null
        }
    }
}
