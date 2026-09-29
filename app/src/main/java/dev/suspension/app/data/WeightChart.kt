package dev.suspension.app.data

import kotlin.math.floor

/**
 * Rider-weight brackets exactly as printed in Fox's fork manuals and on the lower-leg decal:
 * 13 rows of 10 lb, 120–130 lb … 240–250 lb. Looking values up per bracket (instead of
 * interpolating) means the app shows the same number the rider reads on the fork.
 */
object WeightBrackets {
    const val COUNT = 13
    private const val START_LB = 120.0
    private const val STEP_LB = 10.0
    private const val LB_PER_KG = 2.20462262

    /** Kilogram labels as Fox prints them next to the pound ranges. */
    private val KG_RANGES = listOf(
        54 to 59, 59 to 64, 64 to 68, 68 to 73, 73 to 77, 77 to 82, 82 to 86,
        86 to 91, 91 to 95, 95 to 100, 100 to 104, 104 to 109, 109 to 113,
    )

    /** Out-of-table weights clamp to the first/last row — Fox gives no values beyond 54–113 kg. */
    fun indexFor(weightKg: Double): Int =
        floor((weightKg * LB_PER_KG - START_LB) / STEP_LB).toInt().coerceIn(0, COUNT - 1)

    fun kgRange(index: Int): Pair<Int, Int> = KG_RANGES[index]

    fun isCovered(weightKg: Double): Boolean {
        val lb = weightKg * LB_PER_KG
        return lb >= START_LB && lb < START_LB + STEP_LB * COUNT
    }
}

/** One value per [WeightBrackets] row, e.g. air pressure or rebound clicks. */
data class WeightChart(val values: List<Double>) {
    init {
        require(values.size == WeightBrackets.COUNT) { "WeightChart needs ${WeightBrackets.COUNT} rows, got ${values.size}" }
    }

    fun valueFor(weightKg: Double): Double = values[WeightBrackets.indexFor(weightKg)]
}
