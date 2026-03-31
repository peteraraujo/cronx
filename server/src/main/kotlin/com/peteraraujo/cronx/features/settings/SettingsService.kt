package com.peteraraujo.cronx.features.settings

import com.peteraraujo.cronx.db.UserPreferencesTable
import com.peteraraujo.cronx.models.UserPreferences
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.upsert

object SettingsService {
    private const val PREF_KEY = "main_prefs"

    suspend fun getPreferences(): UserPreferences = newSuspendedTransaction {
        val row = UserPreferencesTable.selectAll()
            .where { UserPreferencesTable.prefKey eq PREF_KEY }
            .singleOrNull()

        if (row != null) {
            val jsonString = row[UserPreferencesTable.prefValue]
            Json.decodeFromString(jsonString)
        } else {
            // Return defaults if not set
            UserPreferences()
        }
    }

    suspend fun updatePreferences(newPrefs: UserPreferences) = newSuspendedTransaction {
        val jsonString = Json.encodeToString(newPrefs)


        UserPreferencesTable.upsert {
            it[prefKey] = PREF_KEY
            it[prefValue] = jsonString
        }
    }
}
