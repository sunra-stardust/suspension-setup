package dev.suspension.app.data

import dev.suspension.app.R
import dev.suspension.app.ui.theme.AppColors
import androidx.compose.ui.graphics.Color

/** The seven tuning scenarios, spec §6 — order and index are load-bearing (persistence keys). */
enum class Scenario(val index: Int, val labelResId: Int) {
    BASIS(0, R.string.scenario_basis),
    DOWNHILL(1, R.string.scenario_downhill),
    BIKEPARK(2, R.string.scenario_bikepark),
    TOUR(3, R.string.scenario_tour),
    UPHILL(4, R.string.scenario_uphill),
    KALT(5, R.string.scenario_kalt),
    WARM(6, R.string.scenario_warm),
    ;

    companion object {
        val ordered = entries.sortedBy { it.index }
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
    abstract val hintResId: Int?

    data class Stepper(
        override val id: String,
        override val labelResId: Int,
        val unitResId: Int?,
        override val stripe: Stripe,
        val step: Double,
        val max: Double?,
        val defaults: List<Double>,
        override val hintResId: Int?,
        /** e.g. fork sag: value / travelMm — shown instead of the static hint. */
        val derivedPercentDivisor: Double? = null,
    ) : RowSpec()

    data class Toggle(
        override val id: String,
        override val labelResId: Int,
        override val stripe: Stripe,
        val options: List<String>,
        val defaults: List<String>,
        override val hintResId: Int?,
    ) : RowSpec()
}

/** Static groups resolve a plain string resource; fork/shock resolve a "<model>, <travel> mm" template. */
sealed class GroupHeading {
    data class Static(val resId: Int) : GroupHeading()
    data class Dynamic(val templateResId: Int, val args: List<Any>) : GroupHeading()
}

enum class ComponentKind { FORK, SHOCK }

data class Group(val heading: GroupHeading, val rows: List<RowSpec>, val component: ComponentKind? = null)

/** Per-scenario offsets from the Basis baseline, calibrated at the reference rider weight (spec §6). */
private val PSI_DELTAS = listOf(0.0, 4.0, 12.0, -5.0, 0.0, 5.0, -3.0)
private val FORK_LSC_REFERENCE = listOf(10.0, 8.0, 8.0, 12.0, 10.0, 12.0, 9.0) to 18
private val FORK_HSC_REFERENCE = listOf(5.0, 6.0, 4.0, 6.0, 5.0, 6.0, 5.0) to 8
private val FORK_LSR_REFERENCE = listOf(6.0, 6.0, 5.0, 7.0, 6.0, 7.0, 6.0) to 16
private val FORK_HSR_REFERENCE = listOf(4.0, 4.0, 3.0, 5.0, 4.0, 5.0, 4.0) to 8
private val SHOCK_LSC_REFERENCE = listOf(8.0, 6.0, 6.0, 10.0, 4.0, 10.0, 8.0) to 16
private val SHOCK_HSC_REFERENCE = listOf(5.0, 5.0, 4.0, 6.0, 5.0, 6.0, 5.0) to 8
private val SHOCK_LSR_REFERENCE = listOf(7.0, 7.0, 6.0, 8.0, 7.0, 8.0, 7.0) to 16
private val SHOCK_HSR_REFERENCE = listOf(4.0, 4.0, 3.0, 5.0, 4.0, 5.0, 4.0) to 8

private fun remapAll(reference: Pair<List<Double>, Int>, newMax: Int): List<Double> =
    reference.first.map { remapClick(it, reference.second, newMax) }

/**
 * Spec §6 data table, extended with per-model/per-weight computed defaults for fork & shock
 * (feature-request follow-up: adjustable rider weight + swappable components). Values remain
 * calculated starting points, not manufacturer settings — do not "correct" them (spec §13).
 */
object ScenarioData {

    fun buildForkGroup(fork: ForkModel, weightKg: Double): Group {
        val baselinePsi = interpolateWeightTable(fork.pressureTable, weightKg)
        val psiDefaults = PSI_DELTAS.map { roundToStep(baselinePsi + it, 1.0).coerceAtLeast(0.0) }
        val lscDefaults = remapAll(FORK_LSC_REFERENCE, fork.lscMax)
        val reboundDefaults = remapAll(FORK_LSR_REFERENCE, fork.reboundMax)

        val rows = buildList<RowSpec> {
            add(
                RowSpec.Stepper(
                    id = "f_psi", labelResId = R.string.label_f_psi, unitResId = R.string.unit_psi,
                    stripe = Stripe.SPRING, step = 1.0, max = null, defaults = psiDefaults,
                    hintResId = R.string.hint_f_psi,
                ),
            )
            add(
                RowSpec.Stepper(
                    id = "f_sp", labelResId = R.string.label_f_sp, unitResId = R.string.unit_stk,
                    stripe = Stripe.SPRING, step = 1.0, max = null, defaults = List(7) { 1.0 },
                    hintResId = R.string.hint_f_sp,
                ),
            )
            add(
                RowSpec.Stepper(
                    id = "f_sag", labelResId = R.string.label_sag, unitResId = R.string.unit_mm,
                    stripe = Stripe.SPRING, step = 1.0, max = null,
                    defaults = listOf(32.0, 30.0, 27.0, 34.0, 32.0, 32.0, 32.0),
                    hintResId = null, derivedPercentDivisor = fork.travelMm.toDouble(),
                ),
            )
            add(
                RowSpec.Stepper(
                    id = "f_lsc", labelResId = R.string.label_lsc, unitResId = R.string.unit_von_zu,
                    stripe = Stripe.COMP, step = 1.0, max = fork.lscMax.toDouble(), defaults = lscDefaults,
                    hintResId = R.string.hint_f_lsc,
                ),
            )
            fork.hscMax?.let { hscMax ->
                add(
                    RowSpec.Stepper(
                        id = "f_hsc", labelResId = R.string.label_hsc, unitResId = R.string.unit_von_zu,
                        stripe = Stripe.COMP, step = 1.0, max = hscMax.toDouble(),
                        defaults = remapAll(FORK_HSC_REFERENCE, hscMax), hintResId = R.string.hint_f_hsc,
                    ),
                )
            }
            if (fork.reboundMode == ReboundMode.SPLIT) {
                add(
                    RowSpec.Stepper(
                        id = "f_lsr", labelResId = R.string.label_lsr, unitResId = R.string.unit_von_zu,
                        stripe = Stripe.REB, step = 1.0, max = fork.reboundMax.toDouble(), defaults = reboundDefaults,
                        hintResId = R.string.hint_f_lsr,
                    ),
                )
                fork.hsrMax?.let { hsrMax ->
                    add(
                        RowSpec.Stepper(
                            id = "f_hsr", labelResId = R.string.label_hsr, unitResId = R.string.unit_von_zu,
                            stripe = Stripe.REB, step = 1.0, max = hsrMax.toDouble(),
                            defaults = remapAll(FORK_HSR_REFERENCE, hsrMax), hintResId = R.string.hint_f_hsr,
                        ),
                    )
                }
            } else {
                add(
                    RowSpec.Stepper(
                        id = "f_reb", labelResId = R.string.label_rebound, unitResId = R.string.unit_von_zu,
                        stripe = Stripe.REB, step = 1.0, max = fork.reboundMax.toDouble(), defaults = reboundDefaults,
                        hintResId = R.string.hint_f_reb_single,
                    ),
                )
            }
        }

        return Group(
            heading = GroupHeading.Dynamic(R.string.group_fork_template, listOf(fork.displayName, fork.travelMm)),
            rows = rows,
            component = ComponentKind.FORK,
        )
    }

    fun buildShockGroup(shock: ShockModel, weightKg: Double): Group {
        val baselineRate = shock.referenceRateLbs + shock.rateSlopeLbsPerKg * (weightKg - shock.referenceWeightKg)
        val rateDefault = roundToStep(baselineRate, shock.rateStepLbs).coerceAtLeast(0.0)
        val lscDefaults = remapAll(SHOCK_LSC_REFERENCE, shock.lscMax)
        val reboundDefaults = remapAll(SHOCK_LSR_REFERENCE, shock.reboundMax)

        val rows = buildList<RowSpec> {
            add(
                RowSpec.Stepper(
                    id = "s_rate", labelResId = R.string.label_s_rate, unitResId = R.string.unit_lbs,
                    stripe = Stripe.SPRING, step = shock.rateStepLbs, max = null, defaults = List(7) { rateDefault },
                    hintResId = R.string.hint_s_rate,
                ),
            )
            add(
                RowSpec.Stepper(
                    id = "s_pre", labelResId = R.string.label_s_pre, unitResId = R.string.unit_klicks,
                    stripe = Stripe.SPRING, step = 1.0, max = 26.0,
                    defaults = listOf(8.0, 10.0, 10.0, 6.0, 8.0, 8.0, 8.0), hintResId = R.string.hint_s_pre,
                ),
            )
            add(
                RowSpec.Stepper(
                    id = "s_sag", labelResId = R.string.label_sag, unitResId = R.string.unit_mm,
                    stripe = Stripe.SPRING, step = 0.5, max = null,
                    defaults = listOf(19.5, 18.0, 17.0, 21.0, 18.0, 19.5, 19.5),
                    hintResId = null, derivedPercentDivisor = shock.strokeMm.toDouble(),
                ),
            )
            add(
                RowSpec.Stepper(
                    id = "s_lsc", labelResId = R.string.label_lsc, unitResId = R.string.unit_von_zu,
                    stripe = Stripe.COMP, step = 1.0, max = shock.lscMax.toDouble(), defaults = lscDefaults,
                    hintResId = R.string.hint_s_lsc,
                ),
            )
            shock.hscMax?.let { hscMax ->
                add(
                    RowSpec.Stepper(
                        id = "s_hsc", labelResId = R.string.label_hsc, unitResId = R.string.unit_von_zu,
                        stripe = Stripe.COMP, step = 1.0, max = hscMax.toDouble(),
                        defaults = remapAll(SHOCK_HSC_REFERENCE, hscMax), hintResId = R.string.hint_s_hsc,
                    ),
                )
            }
            if (shock.reboundMode == ReboundMode.SPLIT) {
                add(
                    RowSpec.Stepper(
                        id = "s_lsr", labelResId = R.string.label_lsr, unitResId = R.string.unit_von_zu,
                        stripe = Stripe.REB, step = 1.0, max = shock.reboundMax.toDouble(), defaults = reboundDefaults,
                        hintResId = R.string.hint_s_lsr,
                    ),
                )
                shock.hsrMax?.let { hsrMax ->
                    add(
                        RowSpec.Stepper(
                            id = "s_hsr", labelResId = R.string.label_hsr, unitResId = R.string.unit_von_zu,
                            stripe = Stripe.REB, step = 1.0, max = hsrMax.toDouble(),
                            defaults = remapAll(SHOCK_HSR_REFERENCE, hsrMax), hintResId = R.string.hint_s_hsr,
                        ),
                    )
                }
            } else {
                add(
                    RowSpec.Stepper(
                        id = "s_reb", labelResId = R.string.label_rebound, unitResId = R.string.unit_von_zu,
                        stripe = Stripe.REB, step = 1.0, max = shock.reboundMax.toDouble(), defaults = reboundDefaults,
                        hintResId = R.string.hint_s_reb_single,
                    ),
                )
            }
            add(
                RowSpec.Toggle(
                    id = "s_cs", labelResId = R.string.label_s_cs, stripe = Stripe.SPRING,
                    options = listOf("Offen", "Firm"),
                    defaults = listOf("Offen", "Offen", "Offen", "Offen", "Firm", "Offen", "Offen"),
                    hintResId = R.string.hint_s_cs,
                ),
            )
        }

        return Group(
            heading = GroupHeading.Dynamic(R.string.group_shock_template, listOf(shock.displayName, shock.eyeToEyeMm, shock.strokeMm)),
            rows = rows,
            component = ComponentKind.SHOCK,
        )
    }

    val tiresGroup = Group(
        heading = GroupHeading.Static(R.string.group_tires),
        rows = listOf(
            RowSpec.Stepper(
                id = "t_f", labelResId = R.string.label_t_f, unitResId = R.string.unit_bar,
                stripe = Stripe.NEUTRAL, step = 0.05, max = null,
                defaults = listOf(1.6, 1.6, 1.75, 1.5, 1.6, 1.6, 1.6), hintResId = null,
            ),
            RowSpec.Stepper(
                id = "t_r", labelResId = R.string.label_t_r, unitResId = R.string.unit_bar,
                stripe = Stripe.NEUTRAL, step = 0.05, max = null,
                defaults = listOf(1.8, 1.85, 2.0, 1.7, 1.8, 1.8, 1.8), hintResId = R.string.hint_t_r,
            ),
        ),
    )

    val frameGroup = Group(
        heading = GroupHeading.Static(R.string.group_frame),
        rows = listOf(
            RowSpec.Toggle(
                id = "chip", labelResId = R.string.label_chip, stripe = Stripe.NEUTRAL,
                options = listOf("High", "Low"),
                defaults = listOf("High", "Low", "Low", "High", "High", "High", "High"),
                hintResId = R.string.hint_chip,
            ),
            RowSpec.Stepper(
                id = "post", labelResId = R.string.label_post, unitResId = R.string.unit_psi,
                stripe = Stripe.SPRING, step = 5.0, max = 300.0, defaults = List(7) { 290.0 },
                hintResId = R.string.hint_post,
            ),
        ),
    )
}
