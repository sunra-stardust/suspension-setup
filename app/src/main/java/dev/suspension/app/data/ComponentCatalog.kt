package dev.suspension.app.data

import kotlin.math.roundToInt

/** Whether a damper has one rebound circuit or a split low/high-speed pair. */
enum class ReboundMode { SPLIT, SINGLE }

/**
 * A fork model's tuning envelope. Every number here comes from the manufacturer's own manual
 * (sources in README → "Komponenten-Recherche"); anything we couldn't verify is null and the
 * UI falls back to plain starting values instead of inventing data.
 *
 * Adding a model = adding one entry to [ComponentCatalog.forks]. No other code changes needed.
 */
data class ForkModel(
    val id: String,
    val displayName: String,
    val travelMm: Int,
    val lscMax: Int,
    val hscMax: Int?,
    val reboundMode: ReboundMode,
    /** LSR max when [ReboundMode.SPLIT], the single rebound max otherwise. */
    val reboundMax: Int,
    val hsrMax: Int?,
    /** Manufacturer air-pressure chart by rider weight; null = no verified chart (custom). */
    val pressureChart: WeightChart?,
    /** Pressure used when there's no chart (custom models). */
    val baselinePsi: Double = 100.0,
    val maxPressurePsi: Double?,
    /** Manufacturer rebound starting clicks by rider weight; null = no verified chart. */
    val lsrChart: WeightChart? = null,
    val hsrChart: WeightChart? = null,
    val spacersStock: Int?,
    val spacersMax: Int?,
    /** Short name of the chart's publisher for hints ("Fox"). */
    val chartSource: String? = null,
) {
    val isCustom: Boolean get() = id == CUSTOM_ID
}

/**
 * A coil shock's tuning envelope. There's deliberately no spring-rate-by-weight table here:
 * the right rate depends on the frame's leverage ratio (RockShox says so explicitly), so the
 * recommendation lives in [BikeProfile.springRule], calibrated per frame.
 */
data class ShockModel(
    val id: String,
    val displayName: String,
    val strokeMm: Int,
    val eyeToEyeMm: Int,
    val lscMax: Int,
    val hscMax: Int?,
    val reboundMode: ReboundMode,
    val reboundMax: Int,
    val hsrMax: Int?,
    val hasClimbLever: Boolean,
    /** Installed spring for custom shocks; catalog shocks use the bike's stock spring. */
    val customSpringLbs: Double? = null,
    /** Preload guidance shown as the preload row's hint. */
    val preloadHintResId: Int,
    /** Preload range shown on Basics → Einstellbereiche. */
    val preloadRangeResId: Int,
) {
    val isCustom: Boolean get() = id == CUSTOM_ID
}

const val CUSTOM_ID = "custom"

/**
 * Verified fork and coil-shock catalog (manufacturer manuals, 2025/2026 model years — see
 * README). Deliberately small: a wrong number here is worse than a missing model, and the
 * custom-model entry covers everything else.
 */
object ComponentCatalog {

    /** Fox 36/38 GRIP X2 rebound starting clicks from closed, per rider-weight row (manual 2025). */
    private val FOX_GRIPX2_HSR = WeightChart(listOf(9.0, 8.0, 7.0, 7.0, 7.0, 7.0, 6.0, 5.0, 4.0, 3.0, 3.0, 2.0, 1.0))
    private val FOX_GRIPX2_LSR = WeightChart(listOf(8.0, 7.0, 7.0, 6.0, 6.0, 5.0, 4.0, 4.0, 3.0, 2.0, 1.0, 0.0, 0.0))

    val forks: List<ForkModel> = listOf(
        ForkModel(
            id = "fox38_gripx2",
            displayName = "Fox 38 Factory GRIP X2",
            travelMm = 180,
            lscMax = 18,
            hscMax = 8,
            reboundMode = ReboundMode.SPLIT,
            reboundMax = 16,
            hsrMax = 8,
            // FLOAT column (not E-Bike+) of the 2025 Fox 36/38 manual.
            pressureChart = WeightChart(listOf(72.0, 76.0, 80.0, 84.0, 89.0, 93.0, 97.0, 102.0, 106.0, 110.0, 114.0, 119.0, 123.0)),
            maxPressurePsi = 140.0,
            lsrChart = FOX_GRIPX2_LSR,
            hsrChart = FOX_GRIPX2_HSR,
            // Factory spacer count for 180 mm travel; more spacers = more bottom-out resistance.
            spacersStock = 1,
            spacersMax = 4,
            chartSource = "Fox",
        ),
        ForkModel(
            id = "fox36_gripx2",
            displayName = "Fox 36 Factory GRIP X2",
            travelMm = 160,
            lscMax = 18,
            hscMax = 8,
            reboundMode = ReboundMode.SPLIT,
            reboundMax = 16,
            hsrMax = 8,
            pressureChart = WeightChart(listOf(66.0, 70.0, 74.0, 78.0, 82.0, 86.0, 89.0, 94.0, 99.0, 105.0, 109.0, 113.0, 117.0)),
            maxPressurePsi = 120.0,
            lsrChart = FOX_GRIPX2_LSR,
            hsrChart = FOX_GRIPX2_HSR,
            spacersStock = 1,
            spacersMax = 6,
            chartSource = "Fox",
        ),
    )

    val shocks: List<ShockModel> = listOf(
        ShockModel(
            id = "fox_dhx2_coil",
            displayName = "Fox DHX2 Factory",
            strokeMm = 65,
            eyeToEyeMm = 205,
            lscMax = 16,
            hscMax = 8,
            reboundMode = ReboundMode.SPLIT,
            reboundMax = 16,
            hsrMax = 8,
            hasClimbLever = true,
            preloadHintResId = dev.suspension.app.R.string.hint_s_pre_fox,
            preloadRangeResId = dev.suspension.app.R.string.range_preload_fox,
        ),
        ShockModel(
            id = "fox_dhx2_pe",
            displayName = "Fox DHX2 Performance Elite",
            strokeMm = 65,
            eyeToEyeMm = 205,
            // Fox 2021–2025 manual and Mondraker's spec: LSC and LSR (plus preload) only.
            lscMax = 16,
            hscMax = null,
            reboundMode = ReboundMode.SPLIT,
            reboundMax = 16,
            hsrMax = null,
            hasClimbLever = false,
            preloadHintResId = dev.suspension.app.R.string.hint_s_pre_fox,
            preloadRangeResId = dev.suspension.app.R.string.range_preload_fox,
        ),
        ShockModel(
            id = "rockshox_superdeluxe_coil",
            displayName = "RockShox Super Deluxe Coil Ultimate",
            strokeMm = 65,
            eyeToEyeMm = 205,
            lscMax = 5,
            hscMax = 5,
            reboundMode = ReboundMode.SINGLE,
            reboundMax = 20,
            hsrMax = null,
            hasClimbLever = true,
            preloadHintResId = dev.suspension.app.R.string.hint_s_pre_generic,
            preloadRangeResId = dev.suspension.app.R.string.range_preload_generic,
        ),
        ShockModel(
            id = "rockshox_vivid_coil",
            displayName = "RockShox Vivid Coil Ultimate",
            strokeMm = 65,
            eyeToEyeMm = 205,
            lscMax = 5,
            hscMax = 5,
            reboundMode = ReboundMode.SINGLE,
            reboundMax = 20,
            hsrMax = null,
            hasClimbLever = true,
            preloadHintResId = dev.suspension.app.R.string.hint_s_pre_generic,
            preloadRangeResId = dev.suspension.app.R.string.range_preload_generic,
        ),
    )

    fun forkById(id: String): ForkModel? = forks.find { it.id == id }
    fun shockById(id: String): ShockModel? = shocks.find { it.id == id }
}

/** Preserves a click value's relative position in the old range when switching to a model with a different max. */
fun remapClick(oldValue: Double, oldMax: Int, newMax: Int): Double {
    if (oldMax <= 0) return 0.0
    val fraction = (oldValue / oldMax).coerceIn(0.0, 1.0)
    return (fraction * newMax).roundToInt().toDouble()
}

/** Rounds a computed rate/pressure to the nearest step (e.g. 25 lbs, 1 psi). */
fun roundToStep(value: Double, step: Double): Double {
    if (step <= 0.0) return value
    return kotlin.math.round(value / step) * step
}
