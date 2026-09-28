package dev.suspension.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "scenario_values")

/**
 * Persists edited values only (spec §8) — key `"<paramId>:<scenarioIndex>"`. Anything absent
 * falls back to the caller-supplied default, so a future default change picks up automatically
 * for unedited rows while edited rows stay put.
 */
class ValueRepository(context: Context) {
    private val store = context.applicationContext.dataStore

    private fun key(paramId: String, scenarioIndex: Int) = "$paramId:$scenarioIndex"

    private val safeData: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    fun stepperValue(spec: RowSpec.Stepper, scenario: Scenario): Flow<Double> {
        val prefKey = doublePreferencesKey(key(spec.id, scenario.index))
        val default = spec.defaults[scenario.index]
        return safeData.map { prefs -> prefs[prefKey] ?: default }
    }

    suspend fun setStepperValue(spec: RowSpec.Stepper, scenario: Scenario, value: Double) {
        val prefKey = doublePreferencesKey(key(spec.id, scenario.index))
        val clamped = value.coerceIn(0.0, spec.max ?: Double.MAX_VALUE)
        val rounded = kotlin.math.round(clamped * 100) / 100.0
        store.edit { it[prefKey] = rounded }
    }

    fun toggleValue(spec: RowSpec.Toggle, scenario: Scenario): Flow<String> {
        val prefKey = stringPreferencesKey(key(spec.id, scenario.index))
        val default = spec.defaults[scenario.index]
        return safeData.map { prefs -> prefs[prefKey] ?: default }
    }

    suspend fun setToggleValue(spec: RowSpec.Toggle, scenario: Scenario, value: String) {
        val prefKey = stringPreferencesKey(key(spec.id, scenario.index))
        store.edit { it[prefKey] = value }
    }

    suspend fun resetAll() {
        store.edit { it.clear() }
    }

    /**
     * Drops stored overrides for a component's rows (e.g. all "f_*" keys) — used when switching
     * fork/shock model, since an edit calibrated to the old part's click range is meaningless
     * (and may exceed the new part's max) on the new one.
     */
    suspend fun clearKeysWithPrefix(paramPrefix: String) {
        store.edit { prefs ->
            val toRemove = prefs.asMap().keys.filter { it.name.substringBefore(':').startsWith(paramPrefix) }
            toRemove.forEach { prefs.remove(it) }
        }
    }
}
