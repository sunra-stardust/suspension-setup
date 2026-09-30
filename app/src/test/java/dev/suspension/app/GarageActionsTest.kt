package dev.suspension.app

import dev.suspension.app.data.BikeParts
import dev.suspension.app.data.BikeProfiles
import dev.suspension.app.data.Edit
import dev.suspension.app.data.GarageDoc
import dev.suspension.app.data.GarageJson
import dev.suspension.app.data.RowSpec
import dev.suspension.app.data.ScenarioData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** Phase 3.3: creating, copying, renaming and deleting bikes and Vorlagen. */
class GarageActionsTest {

    private fun doc() = GarageJson.newDocument("b-1", BikeProfiles.levelRr).also {
        GarageDoc.setEdit(it, "b-1", "bikepark", "f_psi", Edit.Delta(12.0))
        GarageDoc.setEdit(it, "b-1", "basis", "s_rate", Edit.Delta(50.0))
    }

    @Test
    fun `copying a bike copies parts and every Vorlage value and selects the copy`() {
        val d = doc()
        GarageDoc.copyBike(d, "b-1", "b-2", "Level RR (Kopie)")
        val garage = GarageJson.parse(d)
        assertEquals("b-2", garage.selectedBike.id)
        assertEquals("Level RR (Kopie)", garage.selectedBike.name)
        assertEquals(BikeProfiles.levelRr.id, garage.selectedBike.profileId)
        assertEquals(Edit.Delta(12.0), garage.selectedBike.editsFor("bikepark")["f_psi"])

        GarageDoc.setEdit(d, "b-2", "bikepark", "f_psi", Edit.Delta(2.0))
        assertEquals(Edit.Delta(12.0), GarageJson.parse(d).bikes.first { it.id == "b-1" }.editsFor("bikepark")["f_psi"], "copies are independent")
    }

    @Test
    fun `a new bike gets the generic frame, the template's parts and installed spring, no edits`() {
        val d = doc()
        val template = GarageJson.parse(d).selectedBike
        val spring = BikeParts.installedSpringLbs(template, 98.0, 20)
        assertEquals(550.0, spring, "stock 500 lbs + the rider's +50 in Basis")

        GarageDoc.addBike(d, "b-2", "Enduro", template, spring)
        val bike = GarageJson.parse(d).selectedBike
        assertEquals("Enduro", bike.name)
        assertEquals(BikeProfiles.GENERIC_ID, bike.profileId)
        assertEquals(template.forkId, bike.forkId)
        assertTrue(bike.edits.isEmpty())

        val profile = BikeParts.profile(bike)
        assertNull(profile.flipChip, "no frame options we couldn't verify")
        assertNull(profile.dropper)
        assertNull(profile.springRule)
        val rate = ScenarioData.buildShockGroup(BikeParts.shock(bike, profile), 98.0, 20, profile).rows
            .filterIsInstance<RowSpec.Stepper>().first { it.id == "s_rate" }
        assertEquals(550.0, rate.start, "starts on the template's spring")
        assertNull(ScenarioData.buildFrameGroup(profile), "no frame group")
    }

    @Test
    fun `bikes can be renamed and deleted, but not the last one`() {
        val d = doc()
        GarageDoc.copyBike(d, "b-1", "b-2", "Zweitrad")
        GarageDoc.renameBike(d, "b-1", "Mondraker")
        assertEquals("Mondraker", GarageJson.parse(d).bikes.first { it.id == "b-1" }.name)

        GarageDoc.deleteBike(d, "b-2")
        val garage = GarageJson.parse(d)
        assertEquals(listOf("b-1"), garage.bikes.map { it.id })
        assertEquals("b-1", garage.selectedBike.id, "selection moves off the deleted bike")
        assertThrows<IllegalArgumentException> { GarageDoc.deleteBike(d, "b-1") }
    }

    @Test
    fun `a new Vorlage copies the source values on every bike and is selected`() {
        val d = doc()
        GarageDoc.copyBike(d, "b-1", "b-2", "Zweitrad")
        GarageDoc.setEdit(d, "b-2", "bikepark", "f_psi", Edit.Delta(8.0))
        GarageDoc.addVorlage(d, "v-1", "Nasse Wurzeln", copyFromId = "bikepark")

        val garage = GarageJson.parse(d)
        assertEquals("v-1", garage.selectedVorlage.id)
        assertEquals("Nasse Wurzeln", garage.selectedVorlage.name)
        assertNull(garage.selectedVorlage.builtIn)
        assertEquals(Edit.Delta(12.0), garage.bikes.first { it.id == "b-1" }.editsFor("v-1")["f_psi"])
        assertEquals(Edit.Delta(8.0), garage.bikes.first { it.id == "b-2" }.editsFor("v-1")["f_psi"])
    }

    @Test
    fun `own Vorlagen can be renamed and deleted, built-ins not`() {
        val d = doc()
        GarageDoc.addVorlage(d, "v-1", "Nasse Wurzeln", copyFromId = "bikepark")
        GarageDoc.renameVorlage(d, "v-1", "Nass")
        assertEquals("Nass", GarageJson.parse(d).vorlagen.first { it.id == "v-1" }.name)

        GarageDoc.deleteVorlage(d, "v-1")
        val garage = GarageJson.parse(d)
        assertFalse(garage.vorlagen.any { it.id == "v-1" })
        assertTrue(garage.selectedBike.editsFor("v-1").isEmpty(), "values removed on every bike")
        assertEquals("basis", garage.selectedVorlage.id)

        assertThrows<IllegalArgumentException> { GarageDoc.renameVorlage(d, "bikepark", "x") }
        assertThrows<IllegalArgumentException> { GarageDoc.deleteVorlage(d, "basis") }
    }
}
