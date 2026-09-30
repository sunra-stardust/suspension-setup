package dev.suspension.app

import dev.suspension.app.data.BikeProfiles
import dev.suspension.app.data.ComponentCatalog
import dev.suspension.app.data.Edit
import dev.suspension.app.data.GarageDoc
import dev.suspension.app.data.GarageJson
import dev.suspension.app.data.RowSpec
import dev.suspension.app.data.RowValues
import dev.suspension.app.data.Scenario
import dev.suspension.app.data.ScenarioData
import dev.suspension.app.data.Stripe
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** garage.json format: parsing, in-place changes, and keeping fields a newer release wrote. */
class GarageJsonTest {

    private val profile = BikeProfiles.levelRr
    private fun fresh() = GarageJson.newDocument("b-1", profile)

    @Test
    fun `a new garage has one stock bike, the built-in Vorlagen and no edits`() {
        val garage = GarageJson.parse(fresh())
        assertEquals(listOf("basis", "downhill", "bikepark", "tour", "uphill"), garage.vorlagen.map { it.id })
        assertEquals(Scenario.ordered, garage.vorlagen.map { it.builtIn })
        assertEquals(profile.stockForkId, garage.selectedBike.forkId)
        assertEquals(profile.stockShockId, garage.selectedBike.shockId)
        assertTrue(garage.selectedBike.edits.isEmpty())
        assertEquals("basis", garage.selectedVorlage.id)
    }

    @Test
    fun `edits are stored per Vorlage and removed again`() {
        val doc = fresh()
        GarageDoc.setEdit(doc, "b-1", "bikepark", "f_psi", Edit.Delta(12.0))
        GarageDoc.setEdit(doc, "b-1", "uphill", "s_cs", Edit.Choice("Firm"))
        var bike = GarageJson.parse(JSONObject(doc.toString())).selectedBike
        assertEquals(Edit.Delta(12.0), bike.editsFor("bikepark")["f_psi"])
        assertEquals(Edit.Choice("Firm"), bike.editsFor("uphill")["s_cs"])

        GarageDoc.setEdit(doc, "b-1", "bikepark", "f_psi", null)
        bike = GarageJson.parse(doc).selectedBike
        assertTrue(bike.editsFor("bikepark").isEmpty())
    }

    @Test
    fun `changing the fork drops only the fork's edits`() {
        val doc = fresh()
        GarageDoc.setEdit(doc, "b-1", "basis", "f_lsc", Edit.Delta(-2.0))
        GarageDoc.setEdit(doc, "b-1", "basis", "s_lsc", Edit.Delta(1.0))
        GarageDoc.selectFork(doc, "b-1", "fox36_gripx2")
        val bike = GarageJson.parse(doc).selectedBike
        assertEquals("fox36_gripx2", bike.forkId)
        assertNull(bike.editsFor("basis")["f_lsc"])
        assertEquals(Edit.Delta(1.0), bike.editsFor("basis")["s_lsc"])
    }

    @Test
    fun `custom parts survive a save`() {
        val doc = fresh()
        val custom = ComponentCatalog.forks.first().copy(id = "custom", displayName = "Öhlins RXF38", hscMax = null, travelMm = 170)
        GarageDoc.selectFork(doc, "b-1", "custom", custom)
        val bike = GarageJson.parse(JSONObject(doc.toString())).selectedBike
        assertEquals("custom", bike.forkId)
        assertEquals("Öhlins RXF38", bike.customFork!!.displayName)
        assertEquals(170, bike.customFork!!.travelMm)
        assertNull(bike.customFork!!.hscMax)
    }

    @Test
    fun `fields from a newer release survive changes made by this one`() {
        val doc = fresh()
        doc.put("backup", JSONObject().put("lastExport", "2027-01-01"))
        GarageDoc.bikeJson(doc, "b-1").put("serviceLog", "future feature")
        doc.getJSONArray("vorlagen").getJSONObject(0).put("icon", "mountain")

        GarageDoc.setEdit(doc, "b-1", "basis", "f_psi", Edit.Delta(2.0))
        GarageDoc.setWeight(doc, 90.0)
        GarageDoc.selectShock(doc, "b-1", "fox_dhx2_coil")

        val saved = JSONObject(doc.toString())
        assertEquals("2027-01-01", saved.getJSONObject("backup").getString("lastExport"))
        assertEquals("future feature", GarageDoc.bikeJson(saved, "b-1").getString("serviceLog"))
        assertEquals("mountain", saved.getJSONArray("vorlagen").getJSONObject(0).getString("icon"))
    }

    @Test
    fun `unknown Vorlage kinds and missing selection fall back safely`() {
        val doc = fresh()
        doc.getJSONArray("vorlagen").put(JSONObject().put("id", "v-x").put("kind", "something-new"))
        doc.remove("selection")
        val garage = GarageJson.parse(doc)
        assertEquals(5, garage.vorlagen.size, "entries this release can't show are skipped")
        assertEquals("basis", garage.selectedVorlage.id)
    }

    @Test
    fun `relative edits clamp to the row's range and vanish at the start value`() {
        val row = RowSpec.Stepper(
            id = "f_lsc", labelResId = R.string.label_lsc, unitResId = null, stripe = Stripe.COMP,
            step = 1.0, max = 18.0, defaults = List(Scenario.count) { 10.0 }, hint = null,
        )
        assertEquals(10.0, RowValues.stepper(row, null))
        assertEquals(Edit.Delta(-2.0), RowValues.editFor(row, 8.0))
        assertNull(RowValues.editFor(row, 10.0), "back at the start value = no edit")
        assertEquals(18.0, RowValues.stepper(row, Edit.Delta(20.0)), "clamped to max")
        assertEquals(0.0, RowValues.stepper(row, Edit.Delta(-20.0)), "clamped to closed")
    }

    @Test
    fun `a toggle edit with an option the row doesn't have shows the start`() {
        val shock = ComponentCatalog.shockById(profile.stockShockId)!!
        val cs = ScenarioData.buildShockGroup(shock, 98.0, 20, profile).rows.filterIsInstance<RowSpec.Toggle>().first { it.id == "s_cs" }
        assertEquals("Offen", RowValues.toggle(cs, Edit.Choice("Trail")))
        assertEquals("Firm", RowValues.toggle(cs, Edit.Choice("Firm")))
    }
}
