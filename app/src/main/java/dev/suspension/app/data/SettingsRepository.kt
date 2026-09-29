package dev.suspension.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.suspension.app.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import kotlin.math.roundToInt

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/** Rider weight including riding gear — Fox's charts assume "fully kitted" weight. */
const val DEFAULT_WEIGHT_KG = 98.0
const val MIN_WEIGHT_KG = 30.0
const val MAX_WEIGHT_KG = 180.0

private object Keys {
    val weightKg = doublePreferencesKey("rider_weight_kg")
    val forkId = stringPreferencesKey("selected_fork_id")
    val shockId = stringPreferencesKey("selected_shock_id")

    val customForkName = stringPreferencesKey("custom_fork_name")
    val customForkTravel = intPreferencesKey("custom_fork_travel_mm")
    val customForkLscMax = intPreferencesKey("custom_fork_lsc_max")
    val customForkHscMax = intPreferencesKey("custom_fork_hsc_max") // -1 = none
    val customForkSplit = booleanPreferencesKey("custom_fork_rebound_split")
    val customForkReboundMax = intPreferencesKey("custom_fork_rebound_max")
    val customForkHsrMax = intPreferencesKey("custom_fork_hsr_max") // -1 = none
    val customForkPsi = doublePreferencesKey("custom_fork_psi")

    val customShockName = stringPreferencesKey("custom_shock_name")
    val customShockStroke = intPreferencesKey("custom_shock_stroke_mm")
    val customShockEyeToEye = intPreferencesKey("custom_shock_eye_to_eye_mm")
    val customShockLscMax = intPreferencesKey("custom_shock_lsc_max")
    val customShockHscMax = intPreferencesKey("custom_shock_hsc_max") // -1 = none
    val customShockSplit = booleanPreferencesKey("custom_shock_rebound_split")
    val customShockReboundMax = intPreferencesKey("custom_shock_rebound_max")
    val customShockHsrMax = intPreferencesKey("custom_shock_hsr_max") // -1 = none
    val customShockRate = doublePreferencesKey("custom_shock_rate_lbs")
    val customShockLever = booleanPreferencesKey("custom_shock_climb_lever")
}

/** Global settings: rider weight and the selected fork/shock (catalog or custom). */
class SettingsRepository(context: Context, private val bike: BikeProfile) {
    private val store = context.applicationContext.settingsDataStore

    private val safeData: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    val weightKg: Flow<Double> = safeData.map { it[Keys.weightKg]?.roundToInt()?.toDouble() ?: DEFAULT_WEIGHT_KG }

    suspend fun setWeightKg(value: Double) {
        store.edit { it[Keys.weightKg] = value.roundToInt().toDouble().coerceIn(MIN_WEIGHT_KG, MAX_WEIGHT_KG) }
    }

    val forkId: Flow<String> = safeData.map { it[Keys.forkId] ?: bike.stockForkId }
    val shockId: Flow<String> = safeData.map { it[Keys.shockId] ?: bike.stockShockId }

    suspend fun selectFork(id: String) {
        store.edit { it[Keys.forkId] = id }
    }

    suspend fun selectShock(id: String) {
        store.edit { it[Keys.shockId] = id }
    }

    val customFork: Flow<ForkModel> = safeData.map { prefs ->
        ForkModel(
            id = CUSTOM_ID,
            displayName = prefs[Keys.customForkName]?.takeIf { it.isNotBlank() } ?: "",
            travelMm = prefs[Keys.customForkTravel] ?: 170,
            lscMax = prefs[Keys.customForkLscMax] ?: 16,
            hscMax = (prefs[Keys.customForkHscMax] ?: 8).takeIf { it >= 0 },
            reboundMode = if (prefs[Keys.customForkSplit] ?: true) ReboundMode.SPLIT else ReboundMode.SINGLE,
            reboundMax = prefs[Keys.customForkReboundMax] ?: 16,
            hsrMax = (prefs[Keys.customForkHsrMax] ?: 8).takeIf { it >= 0 },
            pressureChart = null,
            baselinePsi = prefs[Keys.customForkPsi] ?: 100.0,
            maxPressurePsi = null,
            spacersStock = null,
            spacersMax = null,
        )
    }

    suspend fun setCustomFork(model: ForkModel) {
        store.edit { prefs ->
            prefs[Keys.customForkName] = model.displayName
            prefs[Keys.customForkTravel] = model.travelMm
            prefs[Keys.customForkLscMax] = model.lscMax
            prefs[Keys.customForkHscMax] = model.hscMax ?: -1
            prefs[Keys.customForkSplit] = model.reboundMode == ReboundMode.SPLIT
            prefs[Keys.customForkReboundMax] = model.reboundMax
            prefs[Keys.customForkHsrMax] = model.hsrMax ?: -1
            prefs[Keys.customForkPsi] = model.baselinePsi
        }
    }

    val customShock: Flow<ShockModel> = safeData.map { prefs ->
        ShockModel(
            id = CUSTOM_ID,
            displayName = prefs[Keys.customShockName]?.takeIf { it.isNotBlank() } ?: "",
            strokeMm = prefs[Keys.customShockStroke] ?: 65,
            eyeToEyeMm = prefs[Keys.customShockEyeToEye] ?: 205,
            lscMax = prefs[Keys.customShockLscMax] ?: 16,
            hscMax = (prefs[Keys.customShockHscMax] ?: 8).takeIf { it >= 0 },
            reboundMode = if (prefs[Keys.customShockSplit] ?: true) ReboundMode.SPLIT else ReboundMode.SINGLE,
            reboundMax = prefs[Keys.customShockReboundMax] ?: 16,
            hsrMax = (prefs[Keys.customShockHsrMax] ?: 8).takeIf { it >= 0 },
            hasClimbLever = prefs[Keys.customShockLever] ?: true,
            customSpringLbs = prefs[Keys.customShockRate] ?: bike.stockSpringLbs,
            preloadHintResId = R.string.hint_s_pre_generic,
            preloadRangeResId = R.string.range_preload_generic,
        )
    }

    suspend fun setCustomShock(model: ShockModel) {
        store.edit { prefs ->
            prefs[Keys.customShockName] = model.displayName
            prefs[Keys.customShockStroke] = model.strokeMm
            prefs[Keys.customShockEyeToEye] = model.eyeToEyeMm
            prefs[Keys.customShockLscMax] = model.lscMax
            prefs[Keys.customShockHscMax] = model.hscMax ?: -1
            prefs[Keys.customShockSplit] = model.reboundMode == ReboundMode.SPLIT
            prefs[Keys.customShockReboundMax] = model.reboundMax
            prefs[Keys.customShockHsrMax] = model.hsrMax ?: -1
            prefs[Keys.customShockRate] = model.customSpringLbs ?: bike.stockSpringLbs
            prefs[Keys.customShockLever] = model.hasClimbLever
        }
    }
}
