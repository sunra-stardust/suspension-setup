package dev.suspension.app

import dev.suspension.app.data.BikeProfiles
import dev.suspension.app.data.ComponentCatalog
import dev.suspension.app.data.Deviation
import dev.suspension.app.data.RowSpec
import dev.suspension.app.data.ScenarioData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Phase 3.4: deviation from the starting value. A row may only name a manufacturer when its
 * starting value really is that manufacturer's figure — otherwise it says "starting value".
 */
class DeviationTest {

    private val profile = BikeProfiles.levelRr
    private val fox38 = ComponentCatalog.forkById("fox38_gripx2")!!
    private val ownShock = ComponentCatalog.shockById(profile.stockShockId)!!

    private fun forkRow(id: String, fork: dev.suspension.app.data.ForkModel = fox38, temp: Int = 20) =
        ScenarioData.buildForkGroup(fork, 98.0, temp).rows.first { it.id == id }

    private fun shockRow(id: String, shock: dev.suspension.app.data.ShockModel = ownShock, bike: dev.suspension.app.data.BikeProfile = profile) =
        ScenarioData.buildShockGroup(shock, 98.0, 20, bike).rows.first { it.id == id }

    @Test
    fun `damping deviation is counted in clicks towards closed or open`() {
        val lsc = forkRow("f_lsc") as RowSpec.Stepper
        assertNull(Deviation.of(lsc, lsc.start))
        assertEquals(Deviation.Clicks(2, closer = true, referenceMaker = "Fox"), Deviation.of(lsc, lsc.start - 2))
        assertEquals(Deviation.Clicks(1, closer = false, referenceMaker = "Fox"), Deviation.of(lsc, lsc.start + 1))
    }

    @Test
    fun `quantity deviation keeps its sign and unit`() {
        val psi = forkRow("f_psi") as RowSpec.Stepper
        assertEquals(Deviation.Amount(6.0, 1.0, R.string.unit_psi, "Fox"), Deviation.of(psi, psi.start + 6))
    }

    @Test
    fun `values without a manufacturer figure deviate from the starting value`() {
        assertNull((shockRow("s_lsc") as RowSpec.Stepper).referenceMaker, "shock clicks are the app's start values")
        assertNull((forkRow("f_sag") as RowSpec.Stepper).referenceMaker)
        val cs = shockRow("s_cs") as RowSpec.Toggle
        assertEquals(Deviation.Option("Offen", null), Deviation.of(cs, "Firm"))
    }

    @Test
    fun `Fox starts are the Fox manual's values at the baseline temperature`() {
        for (fork in ComponentCatalog.forks) {
            val lsc = forkRow("f_lsc", fork) as RowSpec.Stepper
            if (lsc.referenceMaker != null) assertEquals(fork.lscStart!!.toDouble(), lsc.start, "${fork.id} LSC")
            val hsc = forkRow("f_hsc", fork) as RowSpec.Stepper
            if (hsc.referenceMaker != null) assertEquals(fork.hscStart!!.toDouble(), hsc.start, "${fork.id} HSC")
            val lsr = forkRow("f_lsr", fork) as RowSpec.Stepper
            if (lsr.referenceMaker != null) assertEquals(fork.lsrChart!!.valueFor(98.0), lsr.start, "${fork.id} LSR")
            val psi = forkRow("f_psi", fork) as RowSpec.Stepper
            if (psi.referenceMaker != null) assertEquals(fork.pressureChart!!.valueFor(98.0), psi.start, "${fork.id} psi")
        }
    }

    @Test
    fun `a custom fork has no manufacturer reference`() {
        val custom = fox38.copy(id = "custom", pressureChart = null, lsrChart = null, hsrChart = null, spacersStock = null, lscStart = null, hscStart = null)
        listOf("f_psi", "f_sp", "f_lsc", "f_hsc", "f_lsr", "f_hsr").forEach { id ->
            assertNull(forkRow(id, custom).referenceMaker, id)
        }
    }

    @Test
    fun `installed spring refers to the frame maker, but not on app-created bikes or own shocks`() {
        assertEquals("Mondraker", shockRow("s_rate").referenceMaker)
        assertNull(shockRow("s_rate", bike = BikeProfiles.generic).referenceMaker)
        assertNull(shockRow("s_rate", shock = ownShock.copy(id = "custom")).referenceMaker)
    }

    @Test
    fun `preload refers to Fox only on shocks with Fox's preload guidance`() {
        assertEquals("Fox", shockRow("s_pre").referenceMaker)
        assertNull(shockRow("s_pre", shock = ComponentCatalog.shockById("rockshox_vivid_coil")!!).referenceMaker)
    }
}
