package dev.suspension.app.data

import kotlin.math.roundToInt

/** The app's baseline ("Basis"): manufacturer charts are given for room temperature (Fox: 21–24 °C). */
const val REFERENCE_TEMP_C = 20
const val DEFAULT_TEMP_C = REFERENCE_TEMP_C
const val MIN_TEMP_C = -15
const val MAX_TEMP_C = 40
const val TEMP_STEP_C = 5

/**
 * How the riding temperature changes the setup.
 *
 * - **Gas** (fork air, tyres): the pressure that matters is the one inside at riding temperature
 *   (constant volume → P ∝ absolute temperature). Values are shown as the pressure to fill at
 *   20 °C so the target is reached when the bike sits at [rideTempC]. Someone pumping at the
 *   trailhead uses the target instead — the row hints show both.
 * - **Oil** (all damping circuits): cold oil is thicker and damps more, warm oil less. Clicks are
 *   opened when cold and closed when warm, calibrated on the owner's own cold/warm setups
 *   (≈ −15 K → LSC +2, other circuits +1; +10 K → LSC −1) and consistent with published advice
 *   (below ~5 °C: compression 1–3 clicks open, rebound 1–2 clicks faster).
 */
object TemperatureModel {
    private const val KELVIN = 273.15
    const val ATMOSPHERE_PSI = 14.7
    const val ATMOSPHERE_BAR = 1.013

    private const val LSC_CLICKS_PER_K = 0.13
    private const val OTHER_CLICKS_PER_K = 0.05
    private const val MAX_CLICK_SHIFT = 3.0

    /** Gauge pressure to fill at [REFERENCE_TEMP_C] so the gas reads [targetGauge] at [rideTempC]. */
    fun fillPressure(targetGauge: Double, rideTempC: Int, atmosphere: Double): Double {
        val targetAbs = targetGauge + atmosphere
        return targetAbs * (KELVIN + REFERENCE_TEMP_C) / (KELVIN + rideTempC) - atmosphere
    }

    /**
     * Clicks to add (open) at [rideTempC]; negative when warmer than the baseline. [rangeScale]
     * is the circuit's max relative to the Fox range the rates were calibrated on, so short
     * dials (RockShox: 5 clicks) shift proportionally less.
     */
    fun clickShift(rideTempC: Int, lowSpeedCompression: Boolean, rangeScale: Double): Int {
        val perKelvin = if (lowSpeedCompression) LSC_CLICKS_PER_K else OTHER_CLICKS_PER_K
        val raw = (perKelvin * (REFERENCE_TEMP_C - rideTempC)).coerceIn(-MAX_CLICK_SHIFT, MAX_CLICK_SHIFT)
        return (raw * rangeScale).roundToInt()
    }
}
