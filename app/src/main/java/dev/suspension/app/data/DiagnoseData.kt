package dev.suspension.app.data

import dev.suspension.app.R

data class DiagnoseEntry(
    val symptomResId: Int,
    val actionResId: Int,
    val explanationResId: Int,
    val stripe: Stripe,
    /** Hand-assigned per Change 01 §6 — never derived by parsing [actionResId]'s string. */
    val direction: RotationDirection?,
    /** Shown only for bikes with these traits (motor, maker attribution, flip chip). */
    val cond: List<Cond> = emptyList(),
)

/**
 * Spec §9 table, in document order; `direction` per Change 01 §6. Entries that mention the motor,
 * quote Fox or name the Level RR flip chip have a neutral twin (same symptom slot, same action) for
 * other bikes, or are left out.
 */
object DiagnoseData {
    private val ebike = listOf(has(Trait.EBIKE))
    private val noEbike = listOf(lacks(Trait.EBIKE))
    private val foxFork = listOf(has(Trait.FORK_FOX))
    private val otherFork = listOf(lacks(Trait.FORK_FOX))
    private val foxShock = listOf(has(Trait.SHOCK_FOX), lacks(Trait.SHOCK_AIR))
    private val otherShock = listOf(lacks(Trait.SHOCK_FOX), lacks(Trait.SHOCK_AIR))
    private val airShock = listOf(has(Trait.SHOCK_AIR))
    private val levelChip = listOf(has(Trait.FLIP_CHIP), has(Trait.FRAME_MONDRAKER_LEVEL))

    val entries: List<DiagnoseEntry> = listOf(
        DiagnoseEntry(R.string.diag_symptom_1, R.string.diag_action_1, R.string.diag_explanation_1, Stripe.COMP, RotationDirection.CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_2, R.string.diag_action_2, R.string.diag_explanation_2, Stripe.COMP, RotationDirection.CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_3, R.string.diag_action_3, R.string.diag_explanation_3, Stripe.COMP, RotationDirection.COUNTER_CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_4, R.string.diag_action_4, R.string.diag_explanation_4, Stripe.COMP, RotationDirection.COUNTER_CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_5, R.string.diag_action_5, R.string.diag_explanation_5, Stripe.COMP, RotationDirection.COUNTER_CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_6, R.string.diag_action_6, R.string.diag_explanation_6, Stripe.COMP, RotationDirection.COUNTER_CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_7, R.string.diag_action_7, R.string.diag_explanation_7, Stripe.COMP, RotationDirection.CLOCKWISE, ebike),
        DiagnoseEntry(R.string.diag_symptom_7_generic, R.string.diag_action_7, R.string.diag_explanation_7_generic, Stripe.COMP, RotationDirection.CLOCKWISE, noEbike),
        DiagnoseEntry(R.string.diag_symptom_8, R.string.diag_action_8, R.string.diag_explanation_8, Stripe.REB, RotationDirection.CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_9, R.string.diag_action_9, R.string.diag_explanation_9, Stripe.REB, RotationDirection.CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_10, R.string.diag_action_10, R.string.diag_explanation_10, Stripe.REB, RotationDirection.COUNTER_CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_11, R.string.diag_action_11, R.string.diag_explanation_11, Stripe.SPRING, null, foxFork),
        DiagnoseEntry(R.string.diag_symptom_11, R.string.diag_action_11, R.string.diag_explanation_11_generic, Stripe.SPRING, null, otherFork),
        DiagnoseEntry(R.string.diag_symptom_12, R.string.diag_action_12, R.string.diag_explanation_12, Stripe.SPRING, null, foxShock),
        DiagnoseEntry(R.string.diag_symptom_12, R.string.diag_action_12, R.string.diag_explanation_12_generic, Stripe.SPRING, null, otherShock),
        DiagnoseEntry(R.string.diag_symptom_12, R.string.diag_action_11, R.string.diag_explanation_12_air, Stripe.SPRING, null, airShock),
        DiagnoseEntry(R.string.diag_symptom_13, R.string.diag_action_13, R.string.diag_explanation_13, Stripe.SPRING, null, foxShock),
        DiagnoseEntry(R.string.diag_symptom_13, R.string.diag_action_13, R.string.diag_explanation_13_generic, Stripe.SPRING, null, otherShock),
        DiagnoseEntry(R.string.diag_symptom_13_air, R.string.diag_action_13_air, R.string.diag_explanation_13_air, Stripe.SPRING, null, airShock),
        // Flip-chip advice and the 5 mm figure are the Level RR's geometry.
        DiagnoseEntry(R.string.diag_symptom_14, R.string.diag_action_14, R.string.diag_explanation_14, Stripe.NEUTRAL, null, levelChip),
        DiagnoseEntry(R.string.diag_symptom_15, R.string.diag_action_15, R.string.diag_explanation_15, Stripe.NEUTRAL, null, levelChip),
        DiagnoseEntry(R.string.diag_symptom_16, R.string.diag_action_16, R.string.diag_explanation_16, Stripe.NEUTRAL, null),
        DiagnoseEntry(R.string.diag_symptom_17, R.string.diag_action_17, R.string.diag_explanation_17, Stripe.NEUTRAL, null),
        DiagnoseEntry(R.string.diag_symptom_18, R.string.diag_action_18, R.string.diag_explanation_18, Stripe.COMP, RotationDirection.COUNTER_CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_19, R.string.diag_action_19, R.string.diag_explanation_19, Stripe.SPRING, null),
    )

    fun forBike(bike: BikeTraits): List<DiagnoseEntry> = entries.filter { bike.allows(it.cond) }
}
