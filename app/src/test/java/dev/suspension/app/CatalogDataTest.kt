package dev.suspension.app

import dev.suspension.app.data.CatalogJson
import dev.suspension.app.data.ComponentCatalog
import dev.suspension.app.data.ForkModel
import dev.suspension.app.data.ReboundMode
import dev.suspension.app.data.ShockModel
import dev.suspension.app.data.WeightChart
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * The catalog lives in `catalog/catalog.json`. These tests pin every value that was verified
 * when the catalog still lived in Kotlin (so moving it changed no number) and enforce the
 * source rules: every field names a known source with a retrieval date.
 */
class CatalogDataTest {

    private val gripX2Lsr = WeightChart(listOf(8.0, 7.0, 7.0, 6.0, 6.0, 5.0, 4.0, 4.0, 3.0, 2.0, 1.0, 0.0, 0.0))
    private val gripX2Hsr = WeightChart(listOf(9.0, 8.0, 7.0, 7.0, 7.0, 7.0, 6.0, 5.0, 4.0, 3.0, 3.0, 2.0, 1.0))

    private val expectedForks = listOf(
        ForkModel(
            id = "fox38_gripx2", displayName = "Fox 38 Factory GRIP X2", travelMm = 180,
            lscMax = 18, hscMax = 8, reboundMode = ReboundMode.SPLIT, reboundMax = 16, hsrMax = 8,
            pressureChart = WeightChart(listOf(72.0, 76.0, 80.0, 84.0, 89.0, 93.0, 97.0, 102.0, 106.0, 110.0, 114.0, 119.0, 123.0)),
            maxPressurePsi = 140.0, lsrChart = gripX2Lsr, hsrChart = gripX2Hsr,
            spacersStock = 1, spacersMax = 4, chartSource = "Fox", lscStart = 10, hscStart = 5,
        ),
        ForkModel(
            id = "fox36_gripx2", displayName = "Fox 36 Factory GRIP X2", travelMm = 160,
            lscMax = 18, hscMax = 8, reboundMode = ReboundMode.SPLIT, reboundMax = 16, hsrMax = 8,
            pressureChart = WeightChart(listOf(66.0, 70.0, 74.0, 78.0, 82.0, 86.0, 89.0, 94.0, 99.0, 105.0, 109.0, 113.0, 117.0)),
            maxPressurePsi = 120.0, lsrChart = gripX2Lsr, hsrChart = gripX2Hsr,
            spacersStock = 1, spacersMax = 6, chartSource = "Fox", lscStart = 10, hscStart = 5,
        ),
    )

    private fun shock(id: String, name: String, lsc: Int, hsc: Int?, mode: ReboundMode, reb: Int, hsr: Int?, lever: Boolean, fox: Boolean) =
        ShockModel(
            id = id, displayName = name, strokeMm = 65, eyeToEyeMm = 205,
            lscMax = lsc, hscMax = hsc, reboundMode = mode, reboundMax = reb, hsrMax = hsr, hasClimbLever = lever,
            preloadHintResId = if (fox) R.string.hint_s_pre_fox else R.string.hint_s_pre_generic,
            preloadRangeResId = if (fox) R.string.range_preload_fox else R.string.range_preload_generic,
        )

    private val expectedShocks = listOf(
        shock("fox_dhx2_hsc_lsr", "Fox DHX2 (HSC · LSC · LSR)", 16, 8, ReboundMode.SPLIT, 16, null, lever = true, fox = true),
        shock("fox_dhx2_coil", "Fox DHX2 Factory", 16, 8, ReboundMode.SPLIT, 16, 8, lever = true, fox = true),
        shock("fox_dhx2_pe", "Fox DHX2 Performance Elite", 16, null, ReboundMode.SPLIT, 16, null, lever = false, fox = true),
        shock("rockshox_superdeluxe_coil", "RockShox Super Deluxe Coil Ultimate", 5, 5, ReboundMode.SINGLE, 20, null, lever = true, fox = false),
        shock("rockshox_vivid_coil", "RockShox Vivid Coil Ultimate", 5, 5, ReboundMode.SINGLE, 20, null, lever = true, fox = false),
    )

    @Test
    fun `forks match the verified values`() {
        assertEquals(expectedForks, ComponentCatalog.forks.map { it.copy(modelYears = emptyList(), provenance = emptyMap()) })
    }

    @Test
    fun `shocks match the verified values`() {
        assertEquals(expectedShocks, ComponentCatalog.shocks.map { it.copy(maker = null, modelYears = emptyList(), provenance = emptyMap()) })
    }

    @Test
    fun `every catalog field names a source`() {
        val forkFields = listOf("travelMm", "lscMax", "hscMax", "reboundMode", "reboundMax", "hsrMax", "pressureChart", "maxPressurePsi")
        val shockFields = listOf("strokeMm", "eyeToEyeMm", "lscMax", "hscMax", "reboundMode", "reboundMax", "hsrMax", "hasClimbLever", "preloadGuide")
        val missing = ComponentCatalog.forks.flatMap { f -> (forkFields - f.provenance.keys).map { "${f.id}.$it" } } +
            ComponentCatalog.shocks.flatMap { s -> (shockFields - s.provenance.keys).map { "${s.id}.$it" } }
        assertTrue(missing.isEmpty(), "Fields without source: $missing")
    }

    @Test
    fun `manufacturer charts come from a manufacturer document for the entry's model years`() {
        for (fork in ComponentCatalog.forks) {
            for (chart in listOf("pressureChart", "lsrChart", "hsrChart")) {
                val source = fork.provenance[chart]?.source ?: continue
                assertTrue(source.url != null, "${fork.id}.$chart: chart without a document")
                assertTrue(fork.modelYears.all { it in source.modelYears }, "${fork.id}.$chart: source ${source.id} does not cover ${fork.modelYears}")
            }
        }
    }

    @Test
    fun `the Fox 38 pressure chart cites the 2025 manual`() {
        val provenance = ComponentCatalog.forkById("fox38_gripx2")!!.provenance.getValue("pressureChart")
        assertEquals("fox-fork-3638-2025", provenance.source.id)
        assertEquals(listOf(2025), provenance.source.modelYears)
        assertEquals("2026-09-29", provenance.source.retrieved)
    }

    @Test
    fun `unknown source ids are rejected`() {
        val json = """
            {"schemaVersion":1,"sources":{},"forks":[{"id":"x","name":"X","maker":"M","modelYears":[],
             "travelMm":{"value":150,"source":"nope"}}],"shocks":[]}
        """.trimIndent()
        assertThrows<IllegalStateException> { CatalogJson.parse(json) }
    }

    @Test
    fun `charts with the wrong row count are rejected`() {
        val json = """
            {"schemaVersion":1,"sources":{"s":{"title":"t","url":"u","modelYears":[2025],"retrieved":"2026-01-01"}},
             "forks":[{"id":"x","name":"X","maker":"M","modelYears":[2025],
               "travelMm":{"value":150,"source":"s"},"lscMax":{"value":10,"source":"s"},"hscMax":{"value":null,"source":"s"},
               "reboundMode":{"value":"single","source":"s"},"reboundMax":{"value":10,"source":"s"},"hsrMax":{"value":null,"source":"s"},
               "pressureChart":{"value":[1,2,3],"source":"s"}}],"shocks":[]}
        """.trimIndent()
        assertThrows<IllegalArgumentException> { CatalogJson.parse(json) }
    }
}
