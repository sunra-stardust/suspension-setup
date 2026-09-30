package dev.suspension.app

import dev.suspension.app.data.BikeParts
import dev.suspension.app.data.BikeProfiles
import dev.suspension.app.data.Edit
import dev.suspension.app.data.LegacyStorage
import dev.suspension.app.data.RowSpec
import dev.suspension.app.data.RowValues
import dev.suspension.app.data.Scenario
import dev.suspension.app.data.ScenarioData
import dev.suspension.app.data.TemperatureModel
import dev.suspension.app.data.tag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Phase 3.2 migration: the old storage (absolute values, scenario offsets baked into the
 * defaults) becomes the first bike with relative edits. The rider must see exactly the same
 * numbers after the update, and a rolled-back release must see the latest ones.
 */
class LegacyStorageTest {

    private val profile = BikeProfiles.levelRr

    /** What the old app stored for the owner: settings only, no overrides (all values = old defaults). */
    private val ownerSettings: Map<String, Any> = mapOf(LegacyStorage.WEIGHT to 98.0, LegacyStorage.TEMP to 20)

    private fun rows(bike: dev.suspension.app.data.Bike, weight: Double, temp: Int): List<RowSpec> = listOfNotNull(
        ScenarioData.buildForkGroup(BikeParts.fork(bike, profile), weight, temp),
        ScenarioData.buildShockGroup(BikeParts.shock(bike, profile), weight, temp, profile),
        ScenarioData.buildTiresGroup(profile, temp),
        ScenarioData.buildFrameGroup(profile),
    ).flatMap { it.rows }

    private fun shown(bike: dev.suspension.app.data.Bike, row: RowSpec, scenario: Scenario): Any {
        val edit = bike.editsFor(scenario.tag)[row.id]
        return when (row) {
            is RowSpec.Stepper -> RowValues.stepper(row, edit)
            is RowSpec.Toggle -> RowValues.toggle(row, edit)
        }
    }

    @Test
    fun `after the update every scenario shows what the old app showed`() {
        val imported = LegacyStorage.import(ownerSettings, emptyMap(), profile, "b-1")
        val mismatches = mutableListOf<String>()
        for (row in rows(imported.bike, 98.0, 20)) {
            for (scenario in Scenario.ordered) {
                val old: Any = when (row) {
                    is RowSpec.Stepper -> row.defaults[scenario.index]
                    is RowSpec.Toggle -> row.defaults[scenario.index]
                }
                val now = shown(imported.bike, row, scenario)
                val same = if (old is Double && now is Double) kotlin.math.abs(old - now) < 1e-9 else old == now
                if (!same) mismatches += "${row.id} ${scenario.tag}: old=$old now=$now"
            }
        }
        assertTrue(mismatches.isEmpty(), mismatches.joinToString("\n"))
    }

    @Test
    fun `Basis has no edits, the former offsets become the rider's own changes`() {
        val bike = LegacyStorage.import(ownerSettings, emptyMap(), profile, "b-1").bike
        assertTrue(bike.editsFor(Scenario.BASIS.tag).isEmpty(), "Basis is the manufacturer value: ${bike.editsFor("basis")}")
        assertEquals(Edit.Delta(12.0), bike.editsFor(Scenario.BIKEPARK.tag)["f_psi"], "Bikepark +12 psi")
        assertEquals(Edit.Delta(4.0), bike.editsFor(Scenario.DOWNHILL.tag)["f_psi"], "Downhill +4 psi")
        assertEquals(Edit.Choice("Firm"), bike.editsFor(Scenario.UPHILL.tag)["s_cs"], "climb switch Firm uphill")
    }

    @Test
    fun `stored overrides are kept`() {
        val values = mapOf("f_psi:${Scenario.BIKEPARK.index}" to 125.0, "s_cs:${Scenario.BASIS.index}" to "Firm")
        val bike = LegacyStorage.import(ownerSettings, values, profile, "b-1").bike
        val psi = rows(bike, 98.0, 20).filterIsInstance<RowSpec.Stepper>().first { it.id == "f_psi" }
        assertEquals(125.0, RowValues.stepper(psi, bike.editsFor("bikepark")["f_psi"]))
        assertEquals(Edit.Choice("Firm"), bike.editsFor("basis")["s_cs"])
    }

    @Test
    fun `migrated offsets follow the temperature because edits are relative`() {
        val bike = LegacyStorage.import(ownerSettings, emptyMap(), profile, "b-1").bike
        val psiCold = rows(bike, 98.0, 0).filterIsInstance<RowSpec.Stepper>().first { it.id == "f_psi" }
        val expectedStartCold = Math.round(TemperatureModel.fillPressure(110.0, 0, TemperatureModel.ATMOSPHERE_PSI)).toDouble()
        assertEquals(expectedStartCold, psiCold.start, "Basis at 0 °C = Fox chart, filled at 20 °C")
        assertEquals(expectedStartCold + 12.0, RowValues.stepper(psiCold, bike.editsFor("bikepark")["f_psi"]))
    }

    @Test
    fun `settings and custom parts are imported`() {
        val settings = ownerSettings + mapOf(
            LegacyStorage.WEIGHT to 84.0,
            LegacyStorage.TEMP to 5,
            LegacyStorage.FORK_ID to "custom",
            LegacyStorage.CUSTOM_FORK_NAME to "Öhlins RXF38",
            LegacyStorage.CUSTOM_FORK_TRAVEL to 170,
            LegacyStorage.CUSTOM_FORK_HSC to -1,
            LegacyStorage.SHOCK_ID to "fox_dhx2_coil",
        )
        val imported = LegacyStorage.import(settings, emptyMap(), profile, "b-1")
        assertEquals(84.0, imported.weightKg)
        assertEquals(5, imported.tempC)
        assertEquals("custom", imported.bike.forkId)
        assertEquals("Öhlins RXF38", imported.bike.customFork!!.displayName)
        assertEquals(170, imported.bike.customFork!!.travelMm)
        assertEquals(null, imported.bike.customFork!!.hscMax)
        assertEquals("fox_dhx2_coil", imported.bike.shockId)
        assertEquals(null, imported.bike.customShock, "never had a custom shock")
    }

    @Test
    fun `mirror and re-import round-trip without changing a value`() {
        val bike = LegacyStorage.import(ownerSettings, mapOf("s_lsc:0" to 3.0), profile, "b-1").bike
            .let { it.copy(edits = it.edits + ("tour" to it.editsFor("tour") + ("t_f" to Edit.Delta(-0.1)))) }
        val settings = LegacyStorage.mirrorSettings(bike, 98.0, 20, profile)
        val values = LegacyStorage.mirrorValues(bike, 98.0, 20, profile)
        val again = LegacyStorage.import(settings, values, profile, "b-1").bike
        for (row in rows(bike, 98.0, 20)) {
            for (scenario in Scenario.ordered) {
                assertEquals(shown(bike, row, scenario), shown(again, row, scenario), "${row.id} ${scenario.tag}")
            }
        }
    }

    @Test
    fun `mirror writes every row of every built-in scenario`() {
        val bike = LegacyStorage.import(ownerSettings, emptyMap(), profile, "b-1").bike
        val values = LegacyStorage.mirrorValues(bike, 98.0, 20, profile)
        val rowCount = rows(bike, 98.0, 20).size
        assertEquals(rowCount * Scenario.count, values.size)
        assertEquals(122.0, values["f_psi:${Scenario.BIKEPARK.index}"])
    }

    @Test
    fun `fingerprint ignores order and number formatting`() {
        val a = LegacyStorage.fingerprint(mapOf("x" to 1, "y" to "a"), mapOf("f:0" to 110.0))
        val b = LegacyStorage.fingerprint(mapOf("y" to "a", "x" to 1), mapOf("f:0" to 110.0))
        assertEquals(a, b)
        assertTrue(a != LegacyStorage.fingerprint(mapOf("x" to 1, "y" to "a"), mapOf("f:0" to 111.0)))
    }
}
