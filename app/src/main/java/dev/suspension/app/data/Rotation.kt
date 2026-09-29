package dev.suspension.app.data

/**
 * The only two symbols allowed to mean "physical turn direction at the dial" (Change 01 §2).
 * Never mix with quantity `+`/`−`, and never render as the U+21BA/U+21BB text glyphs — Barlow
 * doesn't cover them and fallback rendering differs per device (§5).
 */
enum class RotationDirection { CLOCKWISE, COUNTER_CLOCKWISE }

/** Tap → stored-value delta for a rotation control (Change 01 §5), clamped to `0..max`. */
object RotationLogic {
    fun nextValue(direction: RotationDirection, current: Double, max: Double): Double {
        val delta = if (direction == RotationDirection.CLOCKWISE) -1.0 else 1.0
        return (current + delta).coerceIn(0.0, max)
    }
}
