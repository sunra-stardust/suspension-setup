package dev.suspension.app

import dev.suspension.app.data.BikeProfiles
import dev.suspension.app.data.ComponentCatalog
import dev.suspension.app.data.REFERENCE_TEMP_C
import dev.suspension.app.data.RowSpec
import dev.suspension.app.data.Scenario
import dev.suspension.app.data.ScenarioData
import dev.suspension.app.data.TextSpec
import dev.suspension.app.data.WeightBrackets
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Pins the app's starting values to the manufacturer data they came from (Fox 36/38 manual
 * 2025, Fox DHX2 manual 2025, Mondraker Level RR spec) so a refactor can't silently drift.
 * All expectations here are at the 20 °C baseline; temperature is covered in [TemperatureTest].
 */
class ScenarioDataTest {

    private val fox38 = ComponentCatalog.forkById("fox38_gripx2")!!
    private val dhx2Pe = ComponentCatalog.shockById("fox_dhx2_pe")!!
    private val bike = BikeProfiles.levelRr
    private val base = REFERENCE_TEMP_C

    private fun stepper(rows: List<RowSpec>, id: String) = rows.filterIsInstance<RowSpec.Stepper>().first { it.id == id }

    @Test
    fun `there are exactly the five terrain scenarios, temperature is not one of them`() {
        assertEquals(5, Scenario.count)
        assertEquals(listOf(0, 1, 2, 3, 4), Scenario.ordered.map { it.index })
    }

    @Test
    fun `every row has one default per scenario`() {
        val groups = listOfNotNull(
            ScenarioData.buildForkGroup(fox38, 98.0, base),
            ScenarioData.buildShockGroup(dhx2Pe, 98.0, base, bike),
            ScenarioData.buildTiresGroup(bike, base),
            ScenarioData.buildFrameGroup(bike),
        )
        groups.flatMap { it.rows }.forEach { row ->
            val size = when (row) {
                is RowSpec.Stepper -> row.defaults.size
                is RowSpec.Toggle -> row.defaults.size
            }
            assertEquals(Scenario.count, size, "row ${row.id}")
        }
    }

    @Test
    fun `weight brackets match the Fox chart rows`() {
        assertEquals(9, WeightBrackets.indexFor(98.0)) // 210–220 lb = 95–100 kg
        assertEquals(95 to 100, WeightBrackets.kgRange(9))
        assertEquals(8, WeightBrackets.indexFor(95.0)) // 209 lb → 200–210 lb row
        assertEquals(0, WeightBrackets.indexFor(40.0))
        assertEquals(12, WeightBrackets.indexFor(150.0))
    }

    @Test
    fun `fox 38 pressure and rebound at 98 kg come from the Fox chart`() {
        val rows = ScenarioData.buildForkGroup(fox38, 98.0, base).rows
        val psi = stepper(rows, "f_psi")
        assertEquals(110.0, psi.defaults[0], "Basis = Fox FLOAT chart, 95–100 kg")
        assertEquals(122.0, psi.defaults[2], "Bikepark = Basis + 12 (owner's scenario offset)")
        assertEquals(140.0, psi.max, "Fox 38 max air pressure")
        assertEquals(2.0, stepper(rows, "f_lsr").defaults[0], "Fox GRIP X2 LSR, 95–100 kg")
        assertEquals(3.0, stepper(rows, "f_hsr").defaults[0], "Fox GRIP X2 HSR, 95–100 kg")
    }

    @Test
    fun `fox 38 compression starts match Fox's recommendation`() {
        val rows = ScenarioData.buildForkGroup(fox38, 98.0, base).rows
        assertEquals(10.0, stepper(rows, "f_lsc").defaults[0])
        assertEquals(5.0, stepper(rows, "f_hsc").defaults[0])
    }

    @Test
    fun `fox 38 spacers start at the factory count and clamp at max`() {
        val spacers = stepper(ScenarioData.buildForkGroup(fox38, 98.0, base).rows, "f_sp")
        assertEquals(1.0, spacers.defaults[0])
        assertEquals(4.0, spacers.max)
    }

    @Test
    fun `sag defaults keep the spec values on the original fork and shock`() {
        val forkSag = stepper(ScenarioData.buildForkGroup(fox38, 98.0, base).rows, "f_sag").defaults
        assertEquals(listOf(32.0, 30.0, 27.0, 34.0, 32.0), forkSag)
        val shockSag = stepper(ScenarioData.buildShockGroup(dhx2Pe, 98.0, base, bike).rows, "s_sag").defaults
        assertEquals(listOf(19.5, 18.0, 17.0, 21.0, 18.0), shockSag)
    }

    private fun shockRowIds(shockId: String) =
        ScenarioData.buildShockGroup(ComponentCatalog.shockById(shockId)!!, 98.0, base, bike).rows.map { it.id }

    @Test
    fun `the bike's standard shock has HSC, LSC and a single rebound adjuster`() {
        val ids = shockRowIds(bike.stockShockId)
        listOf("s_lsc", "s_hsc", "s_lsr", "s_cs").forEach { assertTrue(it in ids, "missing $it") }
        assertFalse("s_hsr" in ids, "the owner's shock has no HSR knob")
        assertFalse("s_reb" in ids, "rebound is one LSR row, not a second generic one")
    }

    @Test
    fun `the single rebound row keeps the owner's clicks and covers landings in its hint`() {
        val rows = ScenarioData.buildShockGroup(ComponentCatalog.shockById(bike.stockShockId)!!, 98.0, base, bike).rows
        assertEquals(listOf(7.0, 7.0, 6.0, 8.0, 7.0), stepper(rows, "s_lsr").defaults)
        assertEquals(TextSpec.Res(R.string.hint_s_reb_single), stepper(rows, "s_lsr").hint)
        assertEquals(listOf(8.0, 6.0, 6.0, 10.0, 4.0), stepper(rows, "s_lsc").defaults)
        assertEquals(listOf(5.0, 5.0, 4.0, 6.0, 5.0), stepper(rows, "s_hsc").defaults)
    }

    @Test
    fun `dhx2 factory has all four adjusters`() {
        val ids = shockRowIds("fox_dhx2_coil")
        listOf("s_lsc", "s_hsc", "s_lsr", "s_hsr").forEach { assertTrue(it in ids, "missing $it") }
    }

    @Test
    fun `dhx2 performance elite shows only the adjusters Fox documents for it`() {
        val ids = shockRowIds("fox_dhx2_pe")
        assertTrue("s_lsc" in ids)
        assertTrue("s_lsr" in ids)
        assertFalse("s_hsc" in ids, "Fox: HSC is Factory Series only")
        assertFalse("s_hsr" in ids, "Fox: HSR is Factory Series only")
    }

    @Test
    fun `spring rate row shows the installed spring, not the recommendation`() {
        val rate = stepper(ScenarioData.buildShockGroup(dhx2Pe, 120.0, base, bike).rows, "s_rate")
        assertEquals(500.0, rate.defaults[0], "stock spring stays the value until the rider changes it")
        assertEquals(550.0, bike.springRule!!.recommendedLbs(98.0))
    }

    @Test
    fun `every catalog chart has one value per weight row`() {
        ComponentCatalog.forks.forEach { fork ->
            listOfNotNull(fork.pressureChart, fork.lsrChart, fork.hsrChart).forEach { chart ->
                assertEquals(WeightBrackets.COUNT, chart.values.size, fork.id)
            }
        }
    }
}
