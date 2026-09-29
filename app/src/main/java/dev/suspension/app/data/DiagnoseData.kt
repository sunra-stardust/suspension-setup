package dev.suspension.app.data

import dev.suspension.app.R

data class DiagnoseEntry(
    val symptomResId: Int,
    val actionResId: Int,
    val explanationResId: Int,
    val stripe: Stripe,
    /** Hand-assigned per Change 01 §6 — never derived by parsing [actionResId]'s string. */
    val direction: RotationDirection?,
)

/** Spec §9 table, in document order; `direction` per Change 01 §6. */
object DiagnoseData {
    val entries: List<DiagnoseEntry> = listOf(
        DiagnoseEntry(R.string.diag_symptom_1, R.string.diag_action_1, R.string.diag_explanation_1, Stripe.COMP, RotationDirection.CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_2, R.string.diag_action_2, R.string.diag_explanation_2, Stripe.COMP, RotationDirection.CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_3, R.string.diag_action_3, R.string.diag_explanation_3, Stripe.COMP, RotationDirection.COUNTER_CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_4, R.string.diag_action_4, R.string.diag_explanation_4, Stripe.COMP, RotationDirection.COUNTER_CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_5, R.string.diag_action_5, R.string.diag_explanation_5, Stripe.COMP, RotationDirection.COUNTER_CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_6, R.string.diag_action_6, R.string.diag_explanation_6, Stripe.COMP, RotationDirection.COUNTER_CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_7, R.string.diag_action_7, R.string.diag_explanation_7, Stripe.COMP, RotationDirection.CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_8, R.string.diag_action_8, R.string.diag_explanation_8, Stripe.REB, RotationDirection.CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_9, R.string.diag_action_9, R.string.diag_explanation_9, Stripe.REB, RotationDirection.CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_10, R.string.diag_action_10, R.string.diag_explanation_10, Stripe.REB, RotationDirection.COUNTER_CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_11, R.string.diag_action_11, R.string.diag_explanation_11, Stripe.SPRING, null),
        DiagnoseEntry(R.string.diag_symptom_12, R.string.diag_action_12, R.string.diag_explanation_12, Stripe.SPRING, null),
        DiagnoseEntry(R.string.diag_symptom_13, R.string.diag_action_13, R.string.diag_explanation_13, Stripe.SPRING, null),
        DiagnoseEntry(R.string.diag_symptom_14, R.string.diag_action_14, R.string.diag_explanation_14, Stripe.NEUTRAL, null),
        DiagnoseEntry(R.string.diag_symptom_15, R.string.diag_action_15, R.string.diag_explanation_15, Stripe.NEUTRAL, null),
        DiagnoseEntry(R.string.diag_symptom_16, R.string.diag_action_16, R.string.diag_explanation_16, Stripe.NEUTRAL, null),
        DiagnoseEntry(R.string.diag_symptom_17, R.string.diag_action_17, R.string.diag_explanation_17, Stripe.NEUTRAL, null),
        DiagnoseEntry(R.string.diag_symptom_18, R.string.diag_action_18, R.string.diag_explanation_18, Stripe.COMP, RotationDirection.COUNTER_CLOCKWISE),
        DiagnoseEntry(R.string.diag_symptom_19, R.string.diag_action_19, R.string.diag_explanation_19, Stripe.SPRING, null),
    )
}
