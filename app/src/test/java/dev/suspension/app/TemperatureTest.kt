package dev.suspension.app

import dev.suspension.app.data.BikeProfiles
import dev.suspension.app.data.ComponentCatalog
import dev.suspension.app.data.MAX_TEMP_C
import dev.suspension.app.data.MIN_TEMP_C
import dev.suspension.app.data.REFERENCE_TEMP_C
import dev.suspension.app.data.RowSpec
import dev.suspension.app.data.ScenarioData
import dev.suspension.app.data.TemperatureModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TemperatureTest {

    private val fox38 = ComponentCatalog.forkById("fox38_gripx2")!!
    private val dhx2Pe = ComponentCatalog.shockById("fox_dhx2_pe")!!
    private val rockshoxCoil = ComponentCatalog.shockById("rockshox_vivid_coil")!!
    private val bike = BikeProfiles.levelRr

    private fun stepper(rows: List<RowSpec>, id: String) = rows.filterIsInstance<RowSpec.Stepper>().first { it.id == id }
    private fun fork(temp: Int) = ScenarioData.buildForkGroup(fox38, 98.0, temp).rows
    private fun shock(temp: Int) = ScenarioData.buildShockGroup(dhx2Pe, 98.0, temp, bike).rows

    @Test
    fun `the baseline temperature changes nothing`() {
        assertEquals(110.0, TemperatureModel.fillPressure(110.0, REFERENCE_TEMP_C, TemperatureModel.ATMOSPHERE_PSI), 1e-9)
        assertEquals(0, TemperatureModel.clickShift(REFERENCE_TEMP_C, true, 1.0))
        assertEquals(0, TemperatureModel.clickShift(REFERENCE_TEMP_C, false, 1.0))
        assertEquals(110.0, stepper(fork(20), "f_psi").defaults[0])
        assertEquals(1.6, stepper(ScenarioData.buildTiresGroup(bike, 20).rows, "t_f").defaults[0], 1e-9)
    }

    @Test
    fun `cold needs more fill pressure, warm less`() {
        val cold = TemperatureModel.fillPressure(110.0, 0, TemperatureModel.ATMOSPHERE_PSI)
        val warm = TemperatureModel.fillPressure(110.0, 40, TemperatureModel.ATMOSPHERE_PSI)
        assertEquals(119.1, cold, 0.1, "gas law on absolute pressure: 110 psi target at 0 °C")
        assertEquals(102.0, warm, 0.1)
        assertTrue(cold > 110.0 && warm < 110.0)
    }

    @Test
    fun `filling at 20 C and cooling to the ride temperature lands on the target`() {
        listOf(-15, -5, 0, 5, 10, 30, 40).forEach { temp ->
            val fill = TemperatureModel.fillPressure(110.0, temp, TemperatureModel.ATMOSPHERE_PSI)
            // Constant volume: P_abs ∝ T_abs.
            val atRide = (fill + TemperatureModel.ATMOSPHERE_PSI) * (273.15 + temp) / (273.15 + 20) - TemperatureModel.ATMOSPHERE_PSI
            assertEquals(110.0, atRide, 1e-9, "at $temp °C")
        }
    }

    @Test
    fun `fork pressure rows follow the temperature and respect the max`() {
        assertEquals(119.0, stepper(fork(0), "f_psi").defaults[0])
        assertEquals(102.0, stepper(fork(40), "f_psi").defaults[0])
        val extreme = stepper(fork(MIN_TEMP_C), "f_psi")
        assertTrue(extreme.defaults.all { it <= extreme.max!! }, "fill pressure never exceeds the fork's max")
    }

    @Test
    fun `tyres follow the temperature too`() {
        val rows = ScenarioData.buildTiresGroup(bike, 0).rows
        assertEquals(2.0, stepper(rows, "t_r").defaults[0], 1e-9, "1.8 bar target at 0 °C → fill ~2.0 bar at 20 °C")
        val hot = stepper(ScenarioData.buildTiresGroup(bike, 40).rows, "t_f").defaults[0]
        assertEquals(1.45, hot, 1e-9, "1.6 bar target at 40 °C → fill less, the warm air will expand")
    }

    @Test
    fun `click shift reproduces the owner's original cold and warm setups`() {
        // Original "Kalt <5°": LSC +2, HSC/rebound +1. Original "Warm >25°": LSC −1, others 0.
        assertEquals(2, TemperatureModel.clickShift(5, lowSpeedCompression = true, rangeScale = 1.0))
        assertEquals(1, TemperatureModel.clickShift(5, lowSpeedCompression = false, rangeScale = 1.0))
        assertEquals(-1, TemperatureModel.clickShift(30, lowSpeedCompression = true, rangeScale = 1.0))
        assertEquals(0, TemperatureModel.clickShift(30, lowSpeedCompression = false, rangeScale = 1.0))
    }

    @Test
    fun `click shift is capped and scales with the dial range`() {
        assertEquals(3, TemperatureModel.clickShift(MIN_TEMP_C, true, 1.0), "at most 3 clicks")
        assertEquals(-3, TemperatureModel.clickShift(MAX_TEMP_C, true, 1.0))
        val short = TemperatureModel.clickShift(5, true, rangeScale = 5.0 / 16.0)
        assertTrue(short in 0..1, "a 5-click dial shifts proportionally less")
    }

    @Test
    fun `cold opens the damping circuits on fork and shock`() {
        val forkCold = fork(5)
        assertEquals(12.0, stepper(forkCold, "f_lsc").defaults[0], "owner's Kalt fork LSC")
        assertEquals(6.0, stepper(forkCold, "f_hsc").defaults[0], "owner's Kalt fork HSC")
        assertEquals(3.0, stepper(forkCold, "f_lsr").defaults[0], "Fox chart LSR 2 + 1")
        val shockCold = shock(5)
        assertEquals(10.0, stepper(shockCold, "s_lsc").defaults[0], "owner's Kalt shock LSC")
        assertEquals(8.0, stepper(shockCold, "s_lsr").defaults[0], "owner's Kalt shock LSR")
    }

    @Test
    fun `warm closes the compression a little and never leaves the dial`() {
        assertEquals(9.0, stepper(fork(30), "f_lsc").defaults[0], "owner's Warm fork LSC")
        val cold = ScenarioData.buildShockGroup(rockshoxCoil, 98.0, MIN_TEMP_C, bike).rows
        val warm = ScenarioData.buildShockGroup(rockshoxCoil, 98.0, MAX_TEMP_C, bike).rows
        listOf(cold, warm).flatMap { it.filterIsInstance<RowSpec.Stepper>() }
            .filter { it.max != null && it.id.startsWith("s_") && it.id != "s_rate" && it.id != "s_pre" && it.id != "s_sag" }
            .forEach { row -> assertTrue(row.defaults.all { it in 0.0..row.max!! }, "${row.id} stays within 0..max") }
    }

    @Test
    fun `terrain offsets stay on top of the temperature`() {
        val psi = stepper(fork(0), "f_psi").defaults
        assertEquals(psi[0] + 12, psi[2], 1.5, "Bikepark still ~12 psi above Basis when cold (gas law scales it slightly)")
    }
}
