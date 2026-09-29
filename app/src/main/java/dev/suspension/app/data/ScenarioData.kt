package dev.suspension.app.data

import androidx.compose.ui.graphics.Color
import dev.suspension.app.R
import dev.suspension.app.ui.format.formatStepValue
import dev.suspension.app.ui.theme.AppColors
import kotlin.math.roundToInt

/**
 * The tuning scenarios (terrain/use). Order and index are load-bearing: they are part of the
 * persistence keys. Temperature is deliberately not a scenario — it's a global setting that
 * applies to every scenario.
 */
enum class Scenario(val index: Int, val labelResId: Int) {
    BASIS(0, R.string.scenario_basis),
    DOWNHILL(1, R.string.scenario_downhill),
    BIKEPARK(2, R.string.scenario_bikepark),
    TOUR(3, R.string.scenario_tour),
    UPHILL(4, R.string.scenario_uphill),
    ;

    companion object {
        val ordered = entries.sortedBy { it.index }
        val count = entries.size
    }
}

/** Semantic stripe colour, spec §4. */
enum class Stripe {
    COMP, REB, SPRING, NEUTRAL;

    fun color(colors: AppColors): Color = when (this) {
        COMP -> colors.comp
        REB -> colors.reb
        SPRING -> colors.spring
        NEUTRAL -> colors.neutral
    }
}

sealed class RowSpec {
    abstract val id: String
    abstract val labelResId: Int
    abstract val stripe: Stripe
    abstract val hint: TextSpec?

    data class Stepper(
        override val id: String,
        override val labelResId: Int,
        val unitResId: Int?,
        override val stripe: Stripe,
        val step: Double,
        val max: Double?,
        val defaults: List<Double>,
        override val hint: TextSpec?,
        /** e.g. fork sag: value / travelMm — shown instead of the static hint. */
        val derivedPercentDivisor: Double? = null,
    ) : RowSpec()

    data class Toggle(
        override val id: String,
        override val labelResId: Int,
        override val stripe: Stripe,
        val options: List<String>,
        val defaults: List<String>,
        override val hint: TextSpec?,
    ) : RowSpec()
}

enum class ComponentKind { FORK, SHOCK }

data class Group(val heading: TextSpec, val rows: List<RowSpec>, val component: ComponentKind? = null)

/**
 * Per-scenario offsets from the Basis value — the owner's terrain tuning (spec §6), kept as
 * offsets so they apply on top of whatever base the manufacturer chart gives for the rider's
 * weight and model. One entry per [Scenario].
 */
private val PSI_DELTAS = listOf(0.0, 4.0, 12.0, -5.0, 0.0)
private val REBOUND_DELTAS = listOf(0.0, 0.0, -1.0, 1.0, 0.0)

/** Owner's sag targets from spec §6, as fractions of travel so they carry over to other models. */
private val FORK_SAG_FRACTION = listOf(32.0, 30.0, 27.0, 34.0, 32.0).map { it / 180.0 }
private val SHOCK_SAG_FRACTION = listOf(19.5, 18.0, 17.0, 21.0, 18.0).map { it / 65.0 }

private val TIRE_FRONT_BAR = listOf(1.6, 1.6, 1.75, 1.5, 1.6)
private val TIRE_REAR_BAR = listOf(1.8, 1.85, 2.0, 1.7, 1.8)

/** Owner's click values (spec §6) at the Fox ranges they were written for (Fox 38 / DHX2). */
private class ClickReference(val values: List<Double>, val refMax: Int, val lowSpeedCompression: Boolean = false)

private val FORK_LSC = ClickReference(listOf(10.0, 8.0, 8.0, 12.0, 10.0), 18, lowSpeedCompression = true)
private val FORK_HSC = ClickReference(listOf(5.0, 6.0, 4.0, 6.0, 5.0), 8)
private val FORK_LSR = ClickReference(listOf(6.0, 6.0, 5.0, 7.0, 6.0), 16)
private val FORK_HSR = ClickReference(listOf(4.0, 4.0, 3.0, 5.0, 4.0), 8)
private val SHOCK_LSC = ClickReference(listOf(8.0, 6.0, 6.0, 10.0, 4.0), 16, lowSpeedCompression = true)
private val SHOCK_HSC = ClickReference(listOf(5.0, 5.0, 4.0, 6.0, 5.0), 8)
private val SHOCK_LSR = ClickReference(listOf(7.0, 7.0, 6.0, 8.0, 7.0), 16)
private val SHOCK_HSR = ClickReference(listOf(4.0, 4.0, 3.0, 5.0, 4.0), 8)

/** The owner's clicks, scaled to this model's range and shifted for the riding temperature. */
private fun clicks(reference: ClickReference, newMax: Int, tempC: Int): List<Double> {
    val shift = TemperatureModel.clickShift(tempC, reference.lowSpeedCompression, newMax.toDouble() / reference.refMax)
    return reference.values.map { (remapClick(it, reference.refMax, newMax) + shift).coerceIn(0.0, newMax.toDouble()) }
}

/** Manufacturer rebound chart + the owner's per-scenario offsets, shifted for temperature. */
private fun chartRebound(chartValue: Double, max: Int, refMax: Int, tempC: Int): List<Double> {
    val shift = TemperatureModel.clickShift(tempC, lowSpeedCompression = false, rangeScale = max.toDouble() / refMax)
    return REBOUND_DELTAS.map { (chartValue + it + shift).coerceIn(0.0, max.toDouble()) }
}

private fun sagDefaults(fractions: List<Double>, travelMm: Int, step: Double): List<Double> =
    fractions.map { roundToStep(it * travelMm, step) }

/**
 * Builds the Setup rows from the selected fork/shock, the rider weight, the riding temperature
 * and the bike profile. Bases come from the manufacturer's chart where one exists; scenario
 * offsets are the owner's. Values are starting points, not manufacturer guarantees.
 */
object ScenarioData {

    fun buildForkGroup(fork: ForkModel, weightKg: Double, tempC: Int): Group {
        val bracket = WeightBrackets.indexFor(weightKg)
        val (kgLow, kgHigh) = WeightBrackets.kgRange(bracket)
        val source = fork.chartSource.orEmpty()
        val atBaseline = tempC == REFERENCE_TEMP_C

        val basePsi = fork.pressureChart?.valueFor(weightKg) ?: fork.baselinePsi
        val maxPsi = fork.maxPressurePsi
        val psiDefaults = PSI_DELTAS.map { delta ->
            val fill = TemperatureModel.fillPressure(basePsi + delta, tempC, TemperatureModel.ATMOSPHERE_PSI)
            roundToStep(fill, 1.0).coerceIn(0.0, maxPsi ?: Double.MAX_VALUE)
        }
        val psiHint = when {
            fork.pressureChart != null && atBaseline ->
                TextSpec.Format(R.string.hint_f_psi_chart, listOf(source, kgLow, kgHigh, basePsi.roundToInt(), (maxPsi ?: 0.0).roundToInt()))
            fork.pressureChart != null ->
                TextSpec.Format(R.string.hint_f_psi_chart_temp, listOf(source, kgLow, kgHigh, basePsi.roundToInt(), (maxPsi ?: 0.0).roundToInt(), tempC))
            atBaseline -> TextSpec.Res(R.string.hint_f_psi_decal)
            else -> TextSpec.Format(R.string.hint_f_psi_decal_temp, listOf(tempC))
        }

        val spacerHint = if (fork.spacersStock != null && fork.spacersMax != null) {
            TextSpec.Format(R.string.hint_f_sp_known, listOf(fork.spacersStock, fork.spacersMax))
        } else {
            TextSpec.Res(R.string.hint_f_sp_generic)
        }

        val rows = buildList<RowSpec> {
            add(
                RowSpec.Stepper(
                    id = "f_psi", labelResId = R.string.label_f_psi, unitResId = R.string.unit_psi,
                    stripe = Stripe.SPRING, step = 1.0, max = maxPsi, defaults = psiDefaults, hint = psiHint,
                ),
            )
            add(
                RowSpec.Stepper(
                    id = "f_sp", labelResId = R.string.label_f_sp, unitResId = R.string.unit_stk,
                    stripe = Stripe.SPRING, step = 1.0, max = fork.spacersMax?.toDouble(),
                    defaults = List(Scenario.count) { (fork.spacersStock ?: 1).toDouble() }, hint = spacerHint,
                ),
            )
            add(
                RowSpec.Stepper(
                    id = "f_sag", labelResId = R.string.label_sag, unitResId = R.string.unit_mm,
                    stripe = Stripe.SPRING, step = 1.0, max = fork.travelMm.toDouble(),
                    defaults = sagDefaults(FORK_SAG_FRACTION, fork.travelMm, 1.0),
                    hint = null, derivedPercentDivisor = fork.travelMm.toDouble(),
                ),
            )
            add(
                RowSpec.Stepper(
                    id = "f_lsc", labelResId = R.string.label_lsc, unitResId = null,
                    stripe = Stripe.COMP, step = 1.0, max = fork.lscMax.toDouble(),
                    defaults = clicks(FORK_LSC, fork.lscMax, tempC), hint = TextSpec.Res(R.string.hint_f_lsc),
                ),
            )
            fork.hscMax?.let { hscMax ->
                add(
                    RowSpec.Stepper(
                        id = "f_hsc", labelResId = R.string.label_hsc, unitResId = null,
                        stripe = Stripe.COMP, step = 1.0, max = hscMax.toDouble(),
                        defaults = clicks(FORK_HSC, hscMax, tempC), hint = TextSpec.Res(R.string.hint_f_hsc),
                    ),
                )
            }
            if (fork.reboundMode == ReboundMode.SPLIT) {
                val lsrChart = fork.lsrChart
                add(
                    RowSpec.Stepper(
                        id = "f_lsr", labelResId = R.string.label_lsr, unitResId = null,
                        stripe = Stripe.REB, step = 1.0, max = fork.reboundMax.toDouble(),
                        defaults = if (lsrChart != null) {
                            chartRebound(lsrChart.valueFor(weightKg), fork.reboundMax, FORK_LSR.refMax, tempC)
                        } else {
                            clicks(FORK_LSR, fork.reboundMax, tempC)
                        },
                        hint = if (lsrChart != null) {
                            TextSpec.Format(R.string.hint_f_lsr_chart, listOf(source, kgLow, kgHigh, lsrChart.valueFor(weightKg).roundToInt()))
                        } else {
                            TextSpec.Res(R.string.hint_f_lsr)
                        },
                    ),
                )
                fork.hsrMax?.let { hsrMax ->
                    val hsrChart = fork.hsrChart
                    add(
                        RowSpec.Stepper(
                            id = "f_hsr", labelResId = R.string.label_hsr, unitResId = null,
                            stripe = Stripe.REB, step = 1.0, max = hsrMax.toDouble(),
                            defaults = if (hsrChart != null) {
                                chartRebound(hsrChart.valueFor(weightKg), hsrMax, FORK_HSR.refMax, tempC)
                            } else {
                                clicks(FORK_HSR, hsrMax, tempC)
                            },
                            hint = if (hsrChart != null) {
                                TextSpec.Format(R.string.hint_f_hsr_chart, listOf(source, kgLow, kgHigh, hsrChart.valueFor(weightKg).roundToInt()))
                            } else {
                                TextSpec.Res(R.string.hint_f_hsr)
                            },
                        ),
                    )
                }
            } else {
                add(
                    RowSpec.Stepper(
                        id = "f_reb", labelResId = R.string.label_rebound, unitResId = null,
                        stripe = Stripe.REB, step = 1.0, max = fork.reboundMax.toDouble(),
                        defaults = clicks(FORK_LSR, fork.reboundMax, tempC), hint = TextSpec.Res(R.string.hint_f_reb_single),
                    ),
                )
            }
        }

        return Group(
            heading = TextSpec.Format(R.string.group_fork_template, listOf(fork.displayName, fork.travelMm)),
            rows = rows,
            component = ComponentKind.FORK,
        )
    }

    fun buildShockGroup(shock: ShockModel, weightKg: Double, tempC: Int, bike: BikeProfile): Group {
        val installedLbs = shock.customSpringLbs ?: bike.stockSpringLbs
        val recommendedLbs = bike.springRule.recommendedLbs(weightKg)
        val rateHint = if (shock.isCustom) {
            TextSpec.Format(R.string.hint_s_rate_custom, listOf(weightKg.roundToInt(), recommendedLbs.roundToInt()))
        } else {
            TextSpec.Format(R.string.hint_s_rate, listOf(bike.stockSpringLbs.roundToInt(), weightKg.roundToInt(), recommendedLbs.roundToInt()))
        }

        val rows = buildList<RowSpec> {
            add(
                RowSpec.Stepper(
                    id = "s_rate", labelResId = R.string.label_s_rate, unitResId = R.string.unit_lbs,
                    stripe = Stripe.SPRING, step = bike.springRule.stepLbs, max = null,
                    defaults = List(Scenario.count) { installedLbs }, hint = rateHint,
                ),
            )
            add(
                RowSpec.Stepper(
                    id = "s_pre", labelResId = R.string.label_s_pre, unitResId = R.string.unit_klicks,
                    stripe = Stripe.SPRING, step = 1.0, max = 26.0,
                    defaults = listOf(8.0, 10.0, 10.0, 6.0, 8.0), hint = TextSpec.Res(shock.preloadHintResId),
                ),
            )
            add(
                RowSpec.Stepper(
                    id = "s_sag", labelResId = R.string.label_sag, unitResId = R.string.unit_mm,
                    stripe = Stripe.SPRING, step = 0.5, max = shock.strokeMm.toDouble(),
                    defaults = sagDefaults(SHOCK_SAG_FRACTION, shock.strokeMm, 0.5),
                    hint = null, derivedPercentDivisor = shock.strokeMm.toDouble(),
                ),
            )
            add(
                RowSpec.Stepper(
                    id = "s_lsc", labelResId = R.string.label_lsc, unitResId = null,
                    stripe = Stripe.COMP, step = 1.0, max = shock.lscMax.toDouble(),
                    defaults = clicks(SHOCK_LSC, shock.lscMax, tempC), hint = TextSpec.Res(R.string.hint_s_lsc),
                ),
            )
            shock.hscMax?.let { hscMax ->
                add(
                    RowSpec.Stepper(
                        id = "s_hsc", labelResId = R.string.label_hsc, unitResId = null,
                        stripe = Stripe.COMP, step = 1.0, max = hscMax.toDouble(),
                        defaults = clicks(SHOCK_HSC, hscMax, tempC), hint = TextSpec.Res(R.string.hint_s_hsc),
                    ),
                )
            }
            if (shock.reboundMode == ReboundMode.SPLIT) {
                add(
                    RowSpec.Stepper(
                        id = "s_lsr", labelResId = R.string.label_lsr, unitResId = null,
                        stripe = Stripe.REB, step = 1.0, max = shock.reboundMax.toDouble(),
                        defaults = clicks(SHOCK_LSR, shock.reboundMax, tempC),
                        // With no HSR knob this one adjuster covers landings too.
                        hint = TextSpec.Res(if (shock.hsrMax == null) R.string.hint_s_reb_single else R.string.hint_s_lsr),
                    ),
                )
                shock.hsrMax?.let { hsrMax ->
                    add(
                        RowSpec.Stepper(
                            id = "s_hsr", labelResId = R.string.label_hsr, unitResId = null,
                            stripe = Stripe.REB, step = 1.0, max = hsrMax.toDouble(),
                            defaults = clicks(SHOCK_HSR, hsrMax, tempC), hint = TextSpec.Res(R.string.hint_s_hsr),
                        ),
                    )
                }
            } else {
                add(
                    RowSpec.Stepper(
                        id = "s_reb", labelResId = R.string.label_rebound, unitResId = null,
                        stripe = Stripe.REB, step = 1.0, max = shock.reboundMax.toDouble(),
                        defaults = clicks(SHOCK_LSR, shock.reboundMax, tempC), hint = TextSpec.Res(R.string.hint_s_reb_single),
                    ),
                )
            }
            if (shock.hasClimbLever) {
                add(
                    RowSpec.Toggle(
                        id = "s_cs", labelResId = R.string.label_s_cs, stripe = Stripe.SPRING,
                        options = listOf("Offen", "Firm"),
                        defaults = listOf("Offen", "Offen", "Offen", "Offen", "Firm"),
                        hint = TextSpec.Res(R.string.hint_s_cs),
                    ),
                )
            }
        }

        return Group(
            heading = TextSpec.Format(R.string.group_shock_template, listOf(shock.displayName, shock.eyeToEyeMm, shock.strokeMm)),
            rows = rows,
            component = ComponentKind.SHOCK,
        )
    }

    fun buildTiresGroup(bike: BikeProfile, tempC: Int): Group {
        fun fill(targets: List<Double>) =
            targets.map { roundToStep(TemperatureModel.fillPressure(it, tempC, TemperatureModel.ATMOSPHERE_BAR), 0.05) }

        val atBaseline = tempC == REFERENCE_TEMP_C
        val frontHint = if (atBaseline) {
            null
        } else {
            TextSpec.Format(R.string.hint_t_temp, listOf(tempC, formatStepValue(TIRE_FRONT_BAR.first(), 0.05)))
        }
        val rearHint = when {
            atBaseline -> bike.rearTyreHintResId?.let { TextSpec.Res(it) }
            bike.rearTyreHintResId != null ->
                TextSpec.Format(R.string.hint_t_r_temp, listOf(tempC, formatStepValue(TIRE_REAR_BAR.first(), 0.05)))
            else -> TextSpec.Format(R.string.hint_t_temp, listOf(tempC, formatStepValue(TIRE_REAR_BAR.first(), 0.05)))
        }

        return Group(
            heading = TextSpec.Res(R.string.group_tires),
            rows = listOf(
                RowSpec.Stepper(
                    id = "t_f", labelResId = R.string.label_t_f, unitResId = R.string.unit_bar,
                    stripe = Stripe.NEUTRAL, step = 0.05, max = null,
                    defaults = fill(TIRE_FRONT_BAR), hint = frontHint,
                ),
                RowSpec.Stepper(
                    id = "t_r", labelResId = R.string.label_t_r, unitResId = R.string.unit_bar,
                    stripe = Stripe.NEUTRAL, step = 0.05, max = null,
                    defaults = fill(TIRE_REAR_BAR), hint = rearHint,
                ),
            ),
        )
    }

    /** Null when the bike has neither a flip chip nor an air-sprung dropper to track. */
    fun buildFrameGroup(bike: BikeProfile): Group? {
        val rows = buildList<RowSpec> {
            bike.flipChip?.let { chip ->
                add(
                    RowSpec.Toggle(
                        id = "chip", labelResId = R.string.label_chip, stripe = Stripe.NEUTRAL,
                        options = chip.options, defaults = chip.defaults, hint = TextSpec.Res(chip.hintResId),
                    ),
                )
            }
            bike.dropper?.let { post ->
                add(
                    RowSpec.Stepper(
                        id = "post", labelResId = R.string.label_post, unitResId = R.string.unit_psi,
                        stripe = Stripe.SPRING, step = post.stepPsi, max = post.maxPsi,
                        defaults = List(Scenario.count) { post.defaultPsi }, hint = TextSpec.Res(post.hintResId),
                    ),
                )
            }
        }
        return if (rows.isEmpty()) null else Group(heading = TextSpec.Res(R.string.group_frame), rows = rows)
    }
}
