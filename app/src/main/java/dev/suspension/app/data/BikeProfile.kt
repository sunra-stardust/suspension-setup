package dev.suspension.app.data

import dev.suspension.app.R

/**
 * Coil spring-rate rule of thumb, calibrated per frame (the right rate depends on the frame's
 * leverage ratio, so no component-level table exists). ~±5 lbs per kg around a known-good
 * point, rounded to the spring increments that are actually sold.
 */
data class SpringRule(
    val referenceWeightKg: Double,
    val referenceRateLbs: Double,
    val slopeLbsPerKg: Double,
    val stepLbs: Double,
) {
    fun recommendedLbs(weightKg: Double): Double =
        roundToStep(referenceRateLbs + slopeLbsPerKg * (weightKg - referenceWeightKg), stepLbs).coerceAtLeast(0.0)
}

data class FlipChip(val options: List<String>, val defaults: List<String>, val hintResId: Int)

data class Dropper(val defaultPsi: Double, val maxPsi: Double, val stepPsi: Double, val hintResId: Int, val rangeResId: Int)

/**
 * Everything specific to one bike (frame + its stock parts). The app's generic logic — fork and
 * shock rows, Diagnose, Basics — never mentions a bike by name; it reads this profile.
 *
 * To support another bike later: add a profile here with its stock fork/shock ids, spring,
 * frame options and factory-spec strings.
 */
data class BikeProfile(
    val id: String,
    val nameResId: Int,
    val stockForkId: String,
    val stockShockId: String,
    val stockSpringLbs: Double,
    val springRule: SpringRule,
    val rearTyreHintResId: Int?,
    val flipChip: FlipChip?,
    val dropper: Dropper?,
    /** Label/value string pairs for Basics → "Serie ab Werk". */
    val factorySpec: List<Pair<Int, Int>>,
)

object BikeProfiles {

    /**
     * Mondraker Level RR (2025/26: mullet, Fox 38 GRIP X2 180 mm, Fox DHX2 205×65,
     * OnOff Pija). Stock spring 500 lbs is Mondraker's spec for sizes L/XL.
     */
    val levelRr = BikeProfile(
        id = "mondraker_level_rr",
        nameResId = R.string.bike_level_rr_name,
        stockForkId = "fox38_gripx2",
        // The owner's own unit (HSC + LSC + one rebound adjuster, verified on the bike). Mondraker's
        // spec sheet lists a Performance Elite with LSC/LSR only; that and the Factory (all four
        // adjusters) stay selectable in the picker.
        stockShockId = "fox_dhx2_hsc_lsr",
        stockSpringLbs = 500.0,
        // Owner's calculation for this frame: 550 lbs at 95–100 kg rider weight.
        springRule = SpringRule(referenceWeightKg = 97.5, referenceRateLbs = 550.0, slopeLbsPerKg = 5.0, stepLbs = 25.0),
        rearTyreHintResId = R.string.hint_t_r,
        flipChip = FlipChip(
            options = listOf("High", "Low"),
            defaults = listOf("High", "Low", "Low", "High", "High"),
            hintResId = R.string.hint_chip,
        ),
        dropper = Dropper(defaultPsi = 290.0, maxPsi = 300.0, stepPsi = 5.0, hintResId = R.string.hint_post, rangeResId = R.string.range_post_onoff),
        factorySpec = listOf(
            R.string.basics_factory_row_1_label to R.string.basics_factory_row_1_value,
            R.string.basics_factory_row_2_label to R.string.basics_factory_row_2_value,
            R.string.basics_factory_row_3_label to R.string.basics_factory_row_3_value,
            R.string.basics_factory_row_4_label to R.string.basics_factory_row_4_value,
            R.string.basics_factory_row_5_label to R.string.basics_factory_row_5_value,
            R.string.basics_factory_row_6_label to R.string.basics_factory_row_6_value,
        ),
    )

    val current: BikeProfile = levelRr
}
