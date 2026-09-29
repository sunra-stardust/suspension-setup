package dev.suspension.app

import dev.suspension.app.data.RotationDirection
import dev.suspension.app.data.RotationLogic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Change 01 §11 test 1 — table-driven over all 8 damping params (fork LSC/HSC/LSR/HSR,
 * shock LSC/HSC/LSR/HSR): clockwise gives value − 1, counter-clockwise gives value + 1,
 * both clamped to 0..max.
 */
class RotationLogicTest {

    private data class DampingParam(val name: String, val max: Double)

    private val dampingParams = listOf(
        DampingParam("f_lsc", 18.0),
        DampingParam("f_hsc", 8.0),
        DampingParam("f_lsr", 16.0),
        DampingParam("f_hsr", 8.0),
        DampingParam("s_lsc", 16.0),
        DampingParam("s_hsc", 8.0),
        DampingParam("s_lsr", 16.0),
        DampingParam("s_hsr", 8.0),
    )

    @Test
    fun `clockwise decreases value by one`() {
        for (param in dampingParams) {
            val result = RotationLogic.nextValue(RotationDirection.CLOCKWISE, current = 5.0, max = param.max)
            assertEquals(4.0, result, "clockwise on ${param.name} should decrease by 1")
        }
    }

    @Test
    fun `counter-clockwise increases value by one`() {
        for (param in dampingParams) {
            val result = RotationLogic.nextValue(RotationDirection.COUNTER_CLOCKWISE, current = 5.0, max = param.max)
            assertEquals(6.0, result, "counter-clockwise on ${param.name} should increase by 1")
        }
    }

    @Test
    fun `clockwise clamps at zero`() {
        for (param in dampingParams) {
            val result = RotationLogic.nextValue(RotationDirection.CLOCKWISE, current = 0.0, max = param.max)
            assertEquals(0.0, result, "clockwise on ${param.name} must not go below 0")
        }
    }

    @Test
    fun `counter-clockwise clamps at max`() {
        for (param in dampingParams) {
            val result = RotationLogic.nextValue(RotationDirection.COUNTER_CLOCKWISE, current = param.max, max = param.max)
            assertEquals(param.max, result, "counter-clockwise on ${param.name} must not exceed max")
        }
    }
}
