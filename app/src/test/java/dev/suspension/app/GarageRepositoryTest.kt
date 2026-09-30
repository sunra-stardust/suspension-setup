package dev.suspension.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.suspension.app.data.Edit
import dev.suspension.app.data.GarageRepository
import dev.suspension.app.data.LegacyStorage
import dev.suspension.app.data.LegacyStores
import dev.suspension.app.data.Scenario
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Phase 3.2 on a (Robolectric) device: first start migrates the old DataStores, changes land
 * in garage.json and are mirrored back, and changes a rolled-back release made are picked up.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GarageRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val legacy = LegacyStores(context)
    private val garageFile get() = File(context.filesDir, GarageRepository.FILE_NAME)

    @Before
    fun clearOldStorage() = runBlocking { legacy.clearForTest() }

    @After
    fun tearDown() = GarageRepository.forgetInstance()

    private fun restart(): GarageRepository {
        GarageRepository.forgetInstance()
        return GarageRepository.get(context)
    }

    @Test
    fun `first start migrates the old storage`() = runBlocking {
        legacy.write(
            mapOf(LegacyStorage.WEIGHT to 91.0, LegacyStorage.TEMP to 10),
            mapOf("f_lsc:${Scenario.BASIS.index}" to 7.0),
        )
        val repo = restart()
        val garage = repo.state.value
        assertEquals(91.0, garage.weightKg, 0.0)
        assertEquals(10, garage.tempC)
        assertTrue("migrated edit present", garage.selectedBike.editsFor("basis")["f_lsc"] is Edit.Delta)
        assertTrue("Bikepark offset migrated", garage.selectedBike.editsFor("bikepark")["f_psi"] is Edit.Delta)
        assertTrue(garageFile.exists())
    }

    @Test
    fun `a fresh install starts with the stock bike and no edits`() = runBlocking {
        val repo = restart()
        assertTrue(repo.state.value.selectedBike.edits.isEmpty())
        assertTrue(garageFile.exists())
    }

    @Test
    fun `changes are saved to garage json and mirrored to the old storage`() = runBlocking {
        val repo = restart()
        val bikeId = repo.state.value.selectedBike.id
        repo.setEdit(bikeId, "bikepark", "f_psi", Edit.Delta(12.0))
        repo.setWeight(84.0)
        repo.awaitSaved()

        val saved = JSONObject(garageFile.readText())
        assertEquals(84.0, saved.getJSONObject("rider").getDouble("weightKg"), 0.0)

        val old = legacy.read()
        assertEquals(84.0, old.settings[LegacyStorage.WEIGHT])
        // Fox 38 chart row for 84 kg (82–86 kg) is 97 psi; Bikepark = +12.
        assertEquals(109.0, old.values["f_psi:${Scenario.BIKEPARK.index}"])
        assertEquals(97.0, old.values["f_psi:${Scenario.BASIS.index}"])

        val again = restart().state.value
        assertEquals(Edit.Delta(12.0), again.selectedBike.editsFor("bikepark")["f_psi"])
        assertEquals(84.0, again.weightKg, 0.0)
    }

    @Test
    fun `changes a rolled-back release made are imported on the next start`() = runBlocking {
        val repo = restart()
        repo.setEdit(repo.state.value.selectedBike.id, "tour", "t_f", Edit.Delta(-0.1))
        repo.awaitSaved()

        // The old release changes the Basis fork LSC to 6 and the weight to 80 kg.
        val old = legacy.read()
        legacy.write(
            old.settings + mapOf<String, Any>(LegacyStorage.WEIGHT to 80.0),
            old.values + mapOf<String, Any>("f_lsc:${Scenario.BASIS.index}" to 6.0),
        )

        val garage = restart().state.value
        assertEquals(80.0, garage.weightKg, 0.0)
        assertEquals("Fox start 10 → 6", Edit.Delta(-4.0), garage.selectedBike.editsFor("basis")["f_lsc"])
        assertEquals("edit made in the new release is kept", Edit.Delta(-0.1), garage.selectedBike.editsFor("tour")["t_f"])
    }

    @Test
    fun `an unchanged old storage is not imported again`() = runBlocking {
        val repo = restart()
        val bikeId = repo.state.value.selectedBike.id
        repo.setEdit(bikeId, "basis", "s_lsc", Edit.Delta(2.0))
        repo.awaitSaved()
        val before = garageFile.readText()
        restart()
        assertEquals(before, garageFile.readText())
    }

    @Test
    fun `a damaged garage file falls back to the old storage and is kept for inspection`() = runBlocking {
        val repo = restart()
        repo.setEdit(repo.state.value.selectedBike.id, "bikepark", "f_psi", Edit.Delta(5.0))
        repo.awaitSaved()
        garageFile.writeText("""{ "schemaVersion": 1, "bikes": [] }""")

        val garage = restart().state.value
        assertEquals("recovered from the mirror", Edit.Delta(5.0), garage.selectedBike.editsFor("bikepark")["f_psi"])
        assertTrue(context.filesDir.listFiles()!!.any { it.name.startsWith("${GarageRepository.FILE_NAME}.broken-") })
    }
}
