package dev.suspension.app.data

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * How far a shown value is from the row's starting value — manufacturer figure where
 * [RowSpec.referenceMaker] is set, the app's starting value otherwise. Null when there is no
 * deviation. Damping rows speak in turns of the dial ("2 clicks more closed"), never in +/−
 * (Change 01).
 */
sealed class Deviation {
    abstract val referenceMaker: String?

    /** Damping circuit: [clicks] ≥ 1 more closed ([closer]) or more open than the reference. */
    data class Clicks(val clicks: Int, val closer: Boolean, override val referenceMaker: String?) : Deviation()

    /** Quantity row: signed difference in the row's own unit and step. */
    data class Amount(val delta: Double, val step: Double, val unitResId: Int?, override val referenceMaker: String?) : Deviation()

    /** Toggle: the reference option (the rider chose another one). */
    data class Option(val referenceOption: String, override val referenceMaker: String?) : Deviation()

    companion object {
        /** Damping circuits are the rows with a compression or rebound stripe. */
        fun isDamping(row: RowSpec): Boolean = row.stripe == Stripe.COMP || row.stripe == Stripe.REB

        fun of(row: RowSpec.Stepper, value: Double): Deviation? {
            val delta = value - row.start
            if (abs(delta) < row.step / 2) return null
            return if (isDamping(row)) {
                // Counter counts open clicks from closed: fewer = more closed (clockwise).
                Clicks(abs(delta).roundToInt(), closer = delta < 0, referenceMaker = row.referenceMaker)
            } else {
                Amount(delta, row.step, row.unitResId, row.referenceMaker)
            }
        }

        fun of(row: RowSpec.Toggle, value: String): Deviation? =
            if (value == row.start) null else Option(row.start, row.referenceMaker)
    }
}
