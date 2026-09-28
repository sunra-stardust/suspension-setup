package dev.suspension.app.data

import dev.suspension.app.ui.theme.AppColors
import androidx.compose.ui.graphics.Color

/** The seven tuning scenarios, spec §6 — order and index are load-bearing (persistence keys). */
enum class Scenario(val index: Int, val labelResId: Int) {
    BASIS(0, dev.suspension.app.R.string.scenario_basis),
    DOWNHILL(1, dev.suspension.app.R.string.scenario_downhill),
    BIKEPARK(2, dev.suspension.app.R.string.scenario_bikepark),
    TOUR(3, dev.suspension.app.R.string.scenario_tour),
    UPHILL(4, dev.suspension.app.R.string.scenario_uphill),
    KALT(5, dev.suspension.app.R.string.scenario_kalt),
    WARM(6, dev.suspension.app.R.string.scenario_warm),
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
        /** e.g. fork sag: value / 180 — shown instead of the static hint. */
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

data class Group(val headingResId: Int, val rows: List<RowSpec>)

/**
 * Spec §6 data table verbatim. Values are calculated starting points, not manufacturer
 * settings — do not adjust, round, or "correct" them (spec §13).
 */
object ScenarioData {
    val groups: List<Group> = listOf(
        Group(
            headingResId = dev.suspension.app.R.string.group_fork,
            rows = listOf(
                RowSpec.Stepper(
                    id = "f_psi",
                    labelResId = dev.suspension.app.R.string.label_f_psi,
                    unitResId = dev.suspension.app.R.string.unit_psi,
                    stripe = Stripe.SPRING,
                    step = 1.0,
                    max = null,
                    defaults = listOf(105.0, 109.0, 117.0, 100.0, 105.0, 110.0, 102.0),
                    hintResId = dev.suspension.app.R.string.hint_f_psi,
                ),
                RowSpec.Stepper(
                    id = "f_sp",
                    labelResId = dev.suspension.app.R.string.label_f_sp,
                    unitResId = dev.suspension.app.R.string.unit_stk,
                    stripe = Stripe.SPRING,
                    step = 1.0,
                    max = null,
                    defaults = listOf(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
                    hintResId = dev.suspension.app.R.string.hint_f_sp,
                ),
                RowSpec.Stepper(
                    id = "f_sag",
                    labelResId = dev.suspension.app.R.string.label_sag,
                    unitResId = dev.suspension.app.R.string.unit_mm,
                    stripe = Stripe.SPRING,
                    step = 1.0,
                    max = null,
                    defaults = listOf(32.0, 30.0, 27.0, 34.0, 32.0, 32.0, 32.0),
                    hintResId = null,
                    derivedPercentDivisor = 180.0,
                ),
                RowSpec.Stepper(
                    id = "f_lsc",
                    labelResId = dev.suspension.app.R.string.label_lsc,
                    unitResId = dev.suspension.app.R.string.unit_von_zu,
                    stripe = Stripe.COMP,
                    step = 1.0,
                    max = 18.0,
                    defaults = listOf(10.0, 8.0, 8.0, 12.0, 10.0, 12.0, 9.0),
                    hintResId = dev.suspension.app.R.string.hint_f_lsc,
                ),
                RowSpec.Stepper(
                    id = "f_hsc",
                    labelResId = dev.suspension.app.R.string.label_hsc,
                    unitResId = dev.suspension.app.R.string.unit_von_zu,
                    stripe = Stripe.COMP,
                    step = 1.0,
                    max = 8.0,
                    defaults = listOf(5.0, 6.0, 4.0, 6.0, 5.0, 6.0, 5.0),
                    hintResId = dev.suspension.app.R.string.hint_f_hsc,
                ),
                RowSpec.Stepper(
                    id = "f_lsr",
                    labelResId = dev.suspension.app.R.string.label_lsr,
                    unitResId = dev.suspension.app.R.string.unit_von_zu,
                    stripe = Stripe.REB,
                    step = 1.0,
                    max = 16.0,
                    defaults = listOf(6.0, 6.0, 5.0, 7.0, 6.0, 7.0, 6.0),
                    hintResId = dev.suspension.app.R.string.hint_f_lsr,
                ),
                RowSpec.Stepper(
                    id = "f_hsr",
                    labelResId = dev.suspension.app.R.string.label_hsr,
                    unitResId = dev.suspension.app.R.string.unit_von_zu,
                    stripe = Stripe.REB,
                    step = 1.0,
                    max = 8.0,
                    defaults = listOf(4.0, 4.0, 3.0, 5.0, 4.0, 5.0, 4.0),
                    hintResId = dev.suspension.app.R.string.hint_f_hsr,
                ),
            ),
        ),
        Group(
            headingResId = dev.suspension.app.R.string.group_shock,
            rows = listOf(
                RowSpec.Stepper(
                    id = "s_rate",
                    labelResId = dev.suspension.app.R.string.label_s_rate,
                    unitResId = dev.suspension.app.R.string.unit_lbs,
                    stripe = Stripe.SPRING,
                    step = 25.0,
                    max = null,
                    defaults = List(7) { 500.0 },
                    hintResId = dev.suspension.app.R.string.hint_s_rate,
                ),
                RowSpec.Stepper(
                    id = "s_pre",
                    labelResId = dev.suspension.app.R.string.label_s_pre,
                    unitResId = dev.suspension.app.R.string.unit_klicks,
                    stripe = Stripe.SPRING,
                    step = 1.0,
                    max = 26.0,
                    defaults = listOf(8.0, 10.0, 10.0, 6.0, 8.0, 8.0, 8.0),
                    hintResId = dev.suspension.app.R.string.hint_s_pre,
                ),
                RowSpec.Stepper(
                    id = "s_sag",
                    labelResId = dev.suspension.app.R.string.label_sag,
                    unitResId = dev.suspension.app.R.string.unit_mm,
                    stripe = Stripe.SPRING,
                    step = 0.5,
                    max = null,
                    defaults = listOf(19.5, 18.0, 17.0, 21.0, 18.0, 19.5, 19.5),
                    hintResId = null,
                    derivedPercentDivisor = 65.0,
                ),
                RowSpec.Stepper(
                    id = "s_lsc",
                    labelResId = dev.suspension.app.R.string.label_lsc,
                    unitResId = dev.suspension.app.R.string.unit_von_zu,
                    stripe = Stripe.COMP,
                    step = 1.0,
                    max = 16.0,
                    defaults = listOf(8.0, 6.0, 6.0, 10.0, 4.0, 10.0, 8.0),
                    hintResId = dev.suspension.app.R.string.hint_s_lsc,
                ),
                RowSpec.Stepper(
                    id = "s_hsc",
                    labelResId = dev.suspension.app.R.string.label_hsc,
                    unitResId = dev.suspension.app.R.string.unit_von_zu,
                    stripe = Stripe.COMP,
                    step = 1.0,
                    max = 8.0,
                    defaults = listOf(5.0, 5.0, 4.0, 6.0, 5.0, 6.0, 5.0),
                    hintResId = dev.suspension.app.R.string.hint_s_hsc,
                ),
                RowSpec.Stepper(
                    id = "s_lsr",
                    labelResId = dev.suspension.app.R.string.label_lsr,
                    unitResId = dev.suspension.app.R.string.unit_von_zu,
                    stripe = Stripe.REB,
                    step = 1.0,
                    max = 16.0,
                    defaults = listOf(7.0, 7.0, 6.0, 8.0, 7.0, 8.0, 7.0),
                    hintResId = dev.suspension.app.R.string.hint_s_lsr,
                ),
                RowSpec.Stepper(
                    id = "s_hsr",
                    labelResId = dev.suspension.app.R.string.label_hsr,
                    unitResId = dev.suspension.app.R.string.unit_von_zu,
                    stripe = Stripe.REB,
                    step = 1.0,
                    max = 8.0,
                    defaults = listOf(4.0, 4.0, 3.0, 5.0, 4.0, 5.0, 4.0),
                    hintResId = dev.suspension.app.R.string.hint_s_hsr,
                ),
                RowSpec.Toggle(
                    id = "s_cs",
                    labelResId = dev.suspension.app.R.string.label_s_cs,
                    stripe = Stripe.SPRING,
                    options = listOf("Offen", "Firm"),
                    defaults = listOf("Offen", "Offen", "Offen", "Offen", "Firm", "Offen", "Offen"),
                    hintResId = dev.suspension.app.R.string.hint_s_cs,
                ),
            ),
        ),
        Group(
            headingResId = dev.suspension.app.R.string.group_tires,
            rows = listOf(
                RowSpec.Stepper(
                    id = "t_f",
                    labelResId = dev.suspension.app.R.string.label_t_f,
                    unitResId = dev.suspension.app.R.string.unit_bar,
                    stripe = Stripe.NEUTRAL,
                    step = 0.05,
                    max = null,
                    defaults = listOf(1.6, 1.6, 1.75, 1.5, 1.6, 1.6, 1.6),
                    hintResId = null,
                ),
                RowSpec.Stepper(
                    id = "t_r",
                    labelResId = dev.suspension.app.R.string.label_t_r,
                    unitResId = dev.suspension.app.R.string.unit_bar,
                    stripe = Stripe.NEUTRAL,
                    step = 0.05,
                    max = null,
                    defaults = listOf(1.8, 1.85, 2.0, 1.7, 1.8, 1.8, 1.8),
                    hintResId = dev.suspension.app.R.string.hint_t_r,
                ),
            ),
        ),
        Group(
            headingResId = dev.suspension.app.R.string.group_frame,
            rows = listOf(
                RowSpec.Toggle(
                    id = "chip",
                    labelResId = dev.suspension.app.R.string.label_chip,
                    stripe = Stripe.NEUTRAL,
                    options = listOf("High", "Low"),
                    defaults = listOf("High", "Low", "Low", "High", "High", "High", "High"),
                    hintResId = dev.suspension.app.R.string.hint_chip,
                ),
                RowSpec.Stepper(
                    id = "post",
                    labelResId = dev.suspension.app.R.string.label_post,
                    unitResId = dev.suspension.app.R.string.unit_psi,
                    stripe = Stripe.SPRING,
                    step = 5.0,
                    max = 300.0,
                    defaults = List(7) { 290.0 },
                    hintResId = dev.suspension.app.R.string.hint_post,
                ),
            ),
        ),
    )

    val allStepperSpecs: List<RowSpec.Stepper> = groups.flatMap { it.rows }.filterIsInstance<RowSpec.Stepper>()
    val allToggleSpecs: List<RowSpec.Toggle> = groups.flatMap { it.rows }.filterIsInstance<RowSpec.Toggle>()
}
