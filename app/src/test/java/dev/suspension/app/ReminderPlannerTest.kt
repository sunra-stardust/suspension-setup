package dev.suspension.app

import dev.suspension.app.care.CareData
import dev.suspension.app.care.DueCalculator
import dev.suspension.app.care.DueState
import dev.suspension.app.care.LogEntry
import dev.suspension.app.care.Notified
import dev.suspension.app.care.ReminderPlan
import dev.suspension.app.care.ReminderPlanner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ReminderPlannerTest {

    private val purchase = LocalDate.of(2026, 10, 1)
    private val day1 = LocalDate.of(2026, 11, 15)

    /** Only the chain check is in play: every other task is muted. */
    private fun data(
        odometer: Int,
        notified: Map<String, Notified> = emptyMap(),
        enabled: Boolean = true,
        log: List<LogEntry> = emptyList(),
        overrides: Map<String, Boolean> = ALL_MUTED - "chain_check" + ("chain_check" to true),
    ) = CareData(
        purchaseDate = purchase,
        odometerKm = odometer,
        remindersEnabled = enabled,
        reminderOverrides = overrides,
        notified = notified,
        log = log,
    )

    private fun plan(d: CareData, today: LocalDate, permission: Boolean = true): ReminderPlan =
        ReminderPlanner.plan(d, permission, DueCalculator.evaluateAll(d.dueInput(today)), today)

    @Test
    fun `a new soon notifies once, the same state the next day does not`() {
        val first = plan(data(odometer = 420), day1)
        assertEquals(listOf("chain_check"), first.notify.map { it.task.id })
        assertEquals(Notified(DueState.SOON, day1), first.notified["chain_check"])

        val next = plan(data(odometer = 420, notified = first.notified), day1.plusDays(1))
        assertTrue(next.notify.isEmpty())
        assertEquals(first.notified, next.notified)
    }

    @Test
    fun `soon turning overdue notifies again`() {
        val soon = plan(data(odometer = 420), day1)
        val overdue = plan(data(odometer = 510, notified = soon.notified), day1.plusDays(1))
        assertEquals(DueState.OVERDUE, overdue.notify.single().state)
    }

    @Test
    fun `overdue repeats after 14 days, not before`() {
        val first = plan(data(odometer = 510), day1)
        assertEquals(1, first.notify.size)
        assertTrue(plan(data(odometer = 510, notified = first.notified), day1.plusDays(13)).notify.isEmpty())
        val again = plan(data(odometer = 510, notified = first.notified), day1.plusDays(14))
        assertEquals(1, again.notify.size)
        assertEquals(day1.plusDays(14), again.notified["chain_check"]?.date)
    }

    @Test
    fun `soon does not repeat`() {
        val first = plan(data(odometer = 420), day1)
        assertTrue(plan(data(odometer = 420, notified = first.notified), day1.plusDays(30)).notify.isEmpty())
    }

    @Test
    fun `logging clears the stored state`() {
        val first = plan(data(odometer = 510), day1)
        val log = listOf(LogEntry("x", day1, 510, listOf("chain_check"), "", 0))
        val after = plan(data(odometer = 510, notified = first.notified, log = log), day1.plusDays(1))
        assertTrue(after.notify.isEmpty())
        assertFalse("chain_check" in after.notified)
    }

    @Test
    fun `muted tasks never notify and lose their state`() {
        val muted = data(odometer = 510, notified = mapOf("chain_check" to Notified(DueState.SOON, day1)), overrides = ALL_MUTED)
        val result = plan(muted, day1.plusDays(1))
        assertTrue(result.notify.isEmpty())
        assertTrue(result.notified.isEmpty())
    }

    @Test
    fun `nothing happens when reminders are off or the permission is missing`() {
        val stored = mapOf("headset" to Notified(DueState.SOON, day1))
        val off = plan(data(odometer = 510, notified = stored, enabled = false), day1)
        assertTrue(off.notify.isEmpty())
        assertEquals(stored, off.notified)
        val noPermission = plan(data(odometer = 510, notified = stored), day1, permission = false)
        assertTrue(noPermission.notify.isEmpty())
        assertEquals(stored, noPermission.notified)
    }

    @Test
    fun `the default for a task follows remindByDefault`() {
        // pads is due (overdue) but muted by default; first_inspection is overdue and on by default.
        val d = CareData(purchaseDate = purchase, odometerKm = 320, remindersEnabled = true)
        val ids = plan(d, day1).notify.map { it.task.id }
        assertTrue("first_inspection" in ids)
        assertFalse("pads" in ids)
        assertFalse("post_pressure" in ids)
    }

    @Test
    fun `body names are limited to three with a count for the rest`() {
        val more = { n: Int -> "und $n weitere" }
        assertEquals("A · B", ReminderPlanner.bodyNames(listOf("A", "B"), more))
        assertEquals("A · B · C", ReminderPlanner.bodyNames(listOf("A", "B", "C"), more))
        assertEquals("A · B · C und 2 weitere", ReminderPlanner.bodyNames(listOf("A", "B", "C", "D", "E"), more))
    }

    private companion object {
        val ALL_MUTED: Map<String, Boolean> = dev.suspension.app.care.MaintCatalog.tasks.associate { it.id to false }
    }
}
