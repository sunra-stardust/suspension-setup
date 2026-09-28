package dev.suspension.app.data

import kotlin.math.roundToInt

/** Whether a damper has one rebound circuit or a split low/high-speed pair. */
enum class ReboundMode { SPLIT, SINGLE }

/** One point of a weight → pressure/rate curve, used to interpolate a starting value. */
data class WeightPoint(val kg: Double, val value: Double)

/**
 * A fork model's tuning envelope: click ranges (from the model's manual/reviews), and a
 * weight → air-pressure starting-point curve (from the manufacturer's printed sag chart,
 * where published — see README for sources). Values are starting points, not gospel; Fox
 * itself states ±10 psi off its own chart is normal.
 */
data class ForkModel(
    val id: String,
    val displayName: String,
    val travelMm: Int,
    val lscMax: Int,
    val hscMax: Int?,
    val reboundMode: ReboundMode,
    val reboundMax: Int,
    val hsrMax: Int?,
    /** kg → psi, ascending by kg. Empty for a custom model with no known curve. */
    val pressureTable: List<WeightPoint>,
) {
    val isCustom: Boolean get() = id == CUSTOM_ID
}

/**
 * A coil shock's tuning envelope. Spring-rate-by-weight has no universal manufacturer table
 * (RockShox: leverage ratio and frame design change what a given rate produces), so
 * [referenceWeightKg]/[referenceRateLbs]/[rateSlopeLbsPerKg] encode a rule-of-thumb calibrated
 * to this bike's own known-good point (500 lbs installed, 550 lbs recommended at 95–100 kg,
 * spec §6/§10) rather than a fabricated per-model table.
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
    val referenceWeightKg: Double,
    val referenceRateLbs: Double,
    val rateSlopeLbsPerKg: Double,
    val rateStepLbs: Double,
)

const val CUSTOM_ID = "custom"

/**
 * Researched 2026-era enduro/DH fork and coil-shock catalog. Sources (click counts, travel,
 * pressure charts): see README "Komponenten-Recherche". Not exhaustive — anything else is
 * covered by the custom-model entry.
 */
object ComponentCatalog {

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
            pressureTable = listOf(
                WeightPoint(59.0, 72.0),
                WeightPoint(70.8, 84.0),
                WeightPoint(79.4, 93.0),
                WeightPoint(88.5, 102.0),
                WeightPoint(97.5, 110.0),
                WeightPoint(106.6, 114.0),
                WeightPoint(111.1, 123.0),
            ),
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
            pressureTable = listOf(
                WeightPoint(59.0, 66.0),
                WeightPoint(61.7, 70.0),
                WeightPoint(70.3, 78.0),
                WeightPoint(79.4, 86.0),
                WeightPoint(88.5, 94.0),
                WeightPoint(97.5, 105.0),
                WeightPoint(106.6, 109.0),
                WeightPoint(111.1, 117.0),
            ),
        ),
        ForkModel(
            id = "rockshox_zeb_ultimate",
            displayName = "RockShox ZEB Ultimate (Charger 3)",
            travelMm = 170,
            lscMax = 15,
            hscMax = 5,
            reboundMode = ReboundMode.SINGLE,
            reboundMax = 18,
            hsrMax = null,
            pressureTable = listOf(
                WeightPoint(54.4, 87.0),
                WeightPoint(68.0, 124.0),
                WeightPoint(77.1, 140.0),
                WeightPoint(86.2, 154.0),
                WeightPoint(95.3, 166.0),
            ),
        ),
        ForkModel(
            id = "rockshox_lyrik_ultimate",
            displayName = "RockShox Lyrik Ultimate (Charger 3.1)",
            travelMm = 160,
            lscMax = 15,
            hscMax = 5,
            reboundMode = ReboundMode.SINGLE,
            reboundMax = 20,
            hsrMax = null,
            pressureTable = listOf(
                WeightPoint(54.4, 85.0),
                WeightPoint(68.0, 122.0),
                WeightPoint(77.1, 137.0),
                WeightPoint(86.2, 149.0),
                WeightPoint(104.3, 158.0),
            ),
        ),
    )

    val shocks: List<ShockModel> = listOf(
        ShockModel(
            id = "fox_dhx2_coil",
            displayName = "Fox DHX2 Coil",
            strokeMm = 65,
            eyeToEyeMm = 205,
            lscMax = 16,
            hscMax = 8,
            reboundMode = ReboundMode.SPLIT,
            reboundMax = 16,
            hsrMax = 8,
            referenceWeightKg = 97.5,
            referenceRateLbs = 550.0,
            rateSlopeLbsPerKg = 5.0,
            rateStepLbs = 25.0,
        ),
        ShockModel(
            id = "rockshox_superdeluxe_coil",
            displayName = "RockShox Super Deluxe Ultimate Coil",
            strokeMm = 65,
            eyeToEyeMm = 205,
            lscMax = 5,
            hscMax = 5,
            reboundMode = ReboundMode.SINGLE,
            reboundMax = 20,
            hsrMax = null,
            referenceWeightKg = 97.5,
            referenceRateLbs = 550.0,
            rateSlopeLbsPerKg = 5.0,
            rateStepLbs = 25.0,
        ),
        ShockModel(
            id = "rockshox_vivid_coil",
            displayName = "RockShox Vivid Coil",
            strokeMm = 65,
            eyeToEyeMm = 205,
            lscMax = 5,
            hscMax = 5,
            reboundMode = ReboundMode.SINGLE,
            reboundMax = 20,
            hsrMax = null,
            referenceWeightKg = 97.5,
            referenceRateLbs = 550.0,
            rateSlopeLbsPerKg = 5.0,
            rateStepLbs = 25.0,
        ),
    )

    val defaultFork: ForkModel = forks.first()
    val defaultShock: ShockModel = shocks.first()

    fun forkById(id: String): ForkModel? = forks.find { it.id == id }
    fun shockById(id: String): ShockModel? = shocks.find { it.id == id }
}

/** Linear interpolation over an ascending weight→value table; clamps by extrapolating the nearest segment's slope. */
fun interpolateWeightTable(table: List<WeightPoint>, weightKg: Double): Double {
    if (table.isEmpty()) return 0.0
    if (table.size == 1) return table[0].value
    if (weightKg <= table.first().kg) {
        val (a, b) = table[0] to table[1]
        return extrapolate(a, b, weightKg)
    }
    if (weightKg >= table.last().kg) {
        val (a, b) = table[table.size - 2] to table[table.size - 1]
        return extrapolate(a, b, weightKg)
    }
    for (i in 0 until table.size - 1) {
        val a = table[i]
        val b = table[i + 1]
        if (weightKg in a.kg..b.kg) {
            val t = (weightKg - a.kg) / (b.kg - a.kg)
            return a.value + t * (b.value - a.value)
        }
    }
    return table.last().value
}

private fun extrapolate(a: WeightPoint, b: WeightPoint, weightKg: Double): Double {
    val slope = (b.value - a.value) / (b.kg - a.kg)
    return a.value + slope * (weightKg - a.kg)
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
