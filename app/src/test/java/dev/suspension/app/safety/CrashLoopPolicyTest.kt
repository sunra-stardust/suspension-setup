package dev.suspension.app.safety

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CrashLoopPolicyTest {

    private val min = 60_000L

    @Test
    fun `one crash is not a crash loop`() {
        val history = CrashLoopPolicy.recordCrash(emptyList(), nowMs = 100 * min)
        assertFalse(CrashLoopPolicy.shouldEnterSafeMode(history, nowMs = 100 * min))
    }

    @Test
    fun `two crashes within ten minutes trigger safe mode`() {
        var history = CrashLoopPolicy.recordCrash(emptyList(), nowMs = 100 * min)
        history = CrashLoopPolicy.recordCrash(history, nowMs = 101 * min)
        assertTrue(CrashLoopPolicy.shouldEnterSafeMode(history, nowMs = 101 * min))
    }

    @Test
    fun `crashes far apart don't add up`() {
        var history = CrashLoopPolicy.recordCrash(emptyList(), nowMs = 100 * min)
        history = CrashLoopPolicy.recordCrash(history, nowMs = 130 * min)
        assertEquals(listOf(130 * min), history)
        assertFalse(CrashLoopPolicy.shouldEnterSafeMode(history, nowMs = 130 * min))
    }

    @Test
    fun `safe mode wears off once the crashes are old`() {
        val history = listOf(100 * min, 101 * min)
        assertFalse(CrashLoopPolicy.shouldEnterSafeMode(history, nowMs = 200 * min))
    }

    @Test
    fun `history stays small`() {
        var history = emptyList<Long>()
        repeat(20) { history = CrashLoopPolicy.recordCrash(history, nowMs = 100 * min + it) }
        assertEquals(5, history.size)
    }
}
