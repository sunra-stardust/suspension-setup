package dev.suspension.app

import dev.suspension.app.care.DueCalculator
import dev.suspension.app.care.DueInput
import dev.suspension.app.care.DueState
import dev.suspension.app.care.LogEntry
import dev.suspension.app.care.MaintCatalog
import dev.suspension.app.care.RemainingKind
import dev.suspension.app.care.RemainingPart
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DueCalculatorTest {

    private val purchase = LocalDate.of(2026, 10, 1)
    private val today = LocalDate.of(2026, 11, 15)

    private fun input(
        purchaseDate: LocalDate? = purchase,
        odometer: Int = 320,
        log: List<LogEntry> = emptyList(),
        kmPerHour: Int = 15,
        now: LocalDate = today,
    ) = DueInput(purchaseDate, odometer, kmPerHour, now, log)

    private fun due(id: String, input: DueInput = input()) = DueCalculator.evaluate(MaintCatalog.byId.getValue(id), input)

    private fun entry(date: String, km: Int, vararg ids: String, createdAt: Long = 0) =
        LogEntry("e-$date-$km", LocalDate.parse(date), km, ids.toList(), "", createdAt)

    @ParameterizedTest(name = "{0} is {1}")
    @CsvSource(
        "first_inspection, OVERDUE",
        "screws, OVERDUE",
        "pads, OVERDUE",
        "post_pressure, OVERDUE",
        "chain_check, OK",
        "fork_lower, OK",
        "play_check, OK",
        "brake_bleed, OK",
    )
    fun `fixture states`(id: String, expected: DueState) {
        assertEquals(expected, due(id).state)
    }

    @Test
    fun `due points of the fixture`() {
        assertEquals(LocalDate.of(2026, 11, 1), due("pads").dueDate)
        assertEquals(150, due("post_pressure").dueKm)
        assertEquals(750, due("fork_lower").dueKm)
        assertEquals(430, due("fork_lower").remainingKm)
        assertEquals(LocalDate.of(2027, 1, 1), due("play_check").dueDate)
        assertEquals(47, due("play_check").remainingDays)
        assertEquals(LocalDate.of(2028, 10, 1), due("brake_bleed").dueDate)
        assertEquals(180, due("chain_check").remainingKm)
    }

    @Test
    fun `either part decides, the earlier wins`() {
        // Fork overhaul: 125 h = 1875 km or 12 months. Odometer far below, but the date passed.
        val late = input(odometer = 500, now = LocalDate.of(2027, 10, 2))
        assertEquals(DueState.OVERDUE, due("fork_overhaul", late).state)
        // Both parts are reported.
        assertEquals(2, DueCalculator.remainingParts(due("fork_overhaul")).size)
    }

    @Test
    fun `logging the first inspection finishes it and resets the annual inspection`() {
        val log = listOf(entry("2026-11-10", 310, "first_inspection"))
        val i = input(log = log)
        assertEquals(DueState.DONE, due("first_inspection", i).state)
        val annual = due("annual_inspection", i)
        assertEquals(LocalDate.of(2027, 11, 10), annual.dueDate)
        assertEquals(2310, annual.dueKm)
    }

    @Test
    fun `without a purchase date time-only tasks are unknown but km tasks compute`() {
        val i = input(purchaseDate = null)
        assertEquals(DueState.UNKNOWN, due("headset", i).state)
        assertEquals(DueState.UNKNOWN, due("pads", i).state)
        assertEquals(DueState.OVERDUE, due("post_pressure", i).state)
        assertEquals(DueState.OK, due("chain_check", i).state)
        // Hours and months: the km part alone still decides.
        assertEquals(750 - 320, due("fork_lower", i).remainingKm)
        assertNull(due("fork_overhaul", i).dueDate)
    }

    @Test
    fun `a log entry makes a time task computable without a purchase date`() {
        val i = input(purchaseDate = null, log = listOf(entry("2026-11-01", 300, "headset")))
        assertEquals(LocalDate.of(2027, 5, 1), due("headset", i).dueDate)
    }

    @Test
    fun `plusMonths clamps at the end of the month`() {
        val i = input(purchaseDate = LocalDate.of(2027, 1, 31), now = LocalDate.of(2027, 1, 31))
        assertEquals(LocalDate.of(2027, 2, 28), due("pads", i).dueDate)
    }

    @Test
    fun `the latest log entry by date is the base`() {
        val log = listOf(entry("2026-11-01", 300, "headset"), entry("2026-11-12", 330, "headset"), entry("2026-10-20", 100, "headset"))
        assertEquals(LocalDate.of(2027, 5, 12), due("headset", input(log = log)).dueDate)
    }

    @Test
    fun `first values apply only until the first log entry`() {
        // rear_bearings: first after 6 months, then every 12.
        assertEquals(LocalDate.of(2027, 4, 1), due("rear_bearings").dueDate)
        val logged = input(log = listOf(entry("2026-11-10", 310, "rear_bearings")))
        assertEquals(LocalDate.of(2027, 11, 10), due("rear_bearings", logged).dueDate)
    }

    @Test
    fun `soon begins inside the lead window`() {
        // chain_check due at 500 km, lead 100: from 400 km on.
        assertEquals(DueState.OK, due("chain_check", input(odometer = 399)).state)
        assertEquals(DueState.SOON, due("chain_check", input(odometer = 400)).state)
        assertEquals(DueState.OVERDUE, due("chain_check", input(odometer = 500)).state)
        // play_check: due 2027-01-01, lead 14 days.
        assertEquals(DueState.OK, due("play_check", input(now = LocalDate.of(2026, 12, 17))).state)
        assertEquals(DueState.SOON, due("play_check", input(now = LocalDate.of(2026, 12, 18))).state)
    }

    @Test
    fun `the due list puts overdue before soon and the more urgent first`() {
        val all = DueCalculator.evaluateAll(input(odometer = 460))
        val list = DueCalculator.dueList(all)
        val states = list.map { it.state }
        assertEquals(states.sortedBy { it.ordinal }, states)
        assertEquals(true, list.any { it.task.id == "chain_check" && it.state == DueState.SOON })
    }

    @Test
    fun `km per hour converts hour intervals`() {
        assertEquals(200, due("post_pressure", input(kmPerHour = 20)).dueKm)
        assertEquals(1000, due("fork_lower", input(kmPerHour = 20)).dueKm)
    }

    @Test
    fun `remaining parts count km, days and months with overdue flags`() {
        val d = due("pads") // date part only, overdue by 14 days
        assertEquals(listOf(RemainingPart(RemainingKind.DAYS, 14, true)), DueCalculator.remainingParts(d))
        val chain = DueCalculator.remainingParts(due("chain_check"))
        assertEquals(listOf(RemainingPart(RemainingKind.KM, 180, false)), chain)
        val brakes = DueCalculator.remainingParts(due("brake_bleed"))
        assertEquals(RemainingKind.MONTHS, brakes.single().kind)
        assertEquals(1, DueCalculator.remainingParts(due("play_check", input(now = LocalDate.of(2026, 12, 31)))).single().amount)
        // 119 days stay days, 120 become months.
        val base = LocalDate.of(2027, 1, 1)
        assertEquals(RemainingKind.DAYS, DueCalculator.remainingParts(due("play_check", input(now = base.minusDays(119)))).single().kind)
        assertEquals(RemainingPart(RemainingKind.MONTHS, 4, false), DueCalculator.remainingParts(due("play_check", input(now = base.minusDays(120)))).single())
    }
}
