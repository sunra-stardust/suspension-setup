package dev.suspension.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

// File names of the pre-phase-3 storage — never rename (rolled-back releases read them).
private val Context.legacySettingsStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")
private val Context.legacyValuesStore: DataStore<Preferences> by preferencesDataStore(name = "scenario_values")

/** Reads and mirrors the pre-phase-3 DataStores; the format logic is in [LegacyStorage]. */
class LegacyStores(context: Context) {
    private val settings = context.applicationContext.legacySettingsStore
    private val values = context.applicationContext.legacyValuesStore

    data class Snapshot(val settings: Map<String, Any>, val values: Map<String, Any>) {
        val isEmpty: Boolean get() = settings.isEmpty() && values.isEmpty()
        val fingerprint: String get() = LegacyStorage.fingerprint(settings, values)
    }

    suspend fun read(): Snapshot = Snapshot(settings.snapshot(), values.snapshot())

    /** Replaces the old values store with [valueMap] and updates the old settings; returns what's stored now. */
    suspend fun write(settingsMap: Map<String, Any>, valueMap: Map<String, Any>): Snapshot {
        settings.edit { prefs -> settingsMap.forEach { (k, v) -> prefs.putAny(k, v) } }
        values.edit { prefs ->
            prefs.clear()
            valueMap.forEach { (k, v) -> prefs.putAny(k, v) }
        }
        return read()
    }

    /** Tests only: DataStore instances outlive Robolectric's per-test application. */
    internal suspend fun clearForTest() {
        settings.edit { it.clear() }
        values.edit { it.clear() }
    }

    private suspend fun DataStore<Preferences>.snapshot(): Map<String, Any> =
        data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .first().asMap().mapKeys { it.key.name }

    private fun MutablePreferences.putAny(key: String, value: Any) {
        when (value) {
            is Double -> this[doublePreferencesKey(key)] = value
            is Int -> this[intPreferencesKey(key)] = value
            is Boolean -> this[booleanPreferencesKey(key)] = value
            is String -> this[stringPreferencesKey(key)] = value
            else -> error("Unsupported legacy value type for $key: ${value::class}")
        }
    }
}
