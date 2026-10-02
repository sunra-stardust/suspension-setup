package dev.suspension.app

import dev.suspension.app.data.BikeProfiles
import dev.suspension.app.data.CatalogJson
import dev.suspension.app.data.ComponentCatalog
import dev.suspension.app.data.ForkModel
import dev.suspension.app.data.ReboundMode
import dev.suspension.app.data.ShockModel
import dev.suspension.app.data.SpringType
import dev.suspension.app.data.StockPart
import dev.suspension.app.data.WeightChart
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
        ohlinsRxf38("ohlins_rxf38_m2_170", "Öhlins RXF38 m.2 Air 170"),
        ohlinsRxf38("ohlins_rxf38_m3_170", "Öhlins RXF38 m.3 Air 170"),
    )

    /** Öhlins publishes click ranges but no max pressure and no chart in the app's weight rows. */
    private fun ohlinsRxf38(id: String, name: String) = ForkModel(
        id = id, displayName = name, travelMm = 170,
        lscMax = 16, hscMax = 3, reboundMode = ReboundMode.SINGLE, reboundMax = 16, hsrMax = null,
        pressureChart = null, maxPressurePsi = null, lsrChart = null, hsrChart = null,
        spacersStock = null, spacersMax = null, chartSource = "Öhlins", lscStart = null, hscStart = null,
    )

    private fun shock(id: String, name: String, lsc: Int, hsc: Int?, mode: ReboundMode, reb: Int, hsr: Int?, lever: Boolean, fox: Boolean) =
        ShockModel(
            id = id, displayName = name, strokeMm = 65.0, eyeToEyeMm = 205,
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
        // pressureChart / maxPressurePsi are optional (e.g. Öhlins publishes neither); when present they carry a source like every field
        val forkFields = listOf("travelMm", "lscMax", "hscMax", "reboundMode", "reboundMax", "hsrMax")
        val shockFields = listOf("strokeMm", "eyeToEyeMm", "lscMax", "hscMax", "reboundMode", "reboundMax", "hsrMax", "hasClimbLever", "preloadGuide")
        val missing = ComponentCatalog.forks.flatMap { f -> (forkFields - f.provenance.keys).map { "${f.id}.$it" } } +
            ComponentCatalog.shocks.flatMap { s -> (shockFields - s.provenance.keys).map { "${s.id}.$it" } }
        assertTrue(missing.isEmpty(), "Fields without source: $missing")
        val foxWithoutChart = ComponentCatalog.forks.filter { it.chartSource == "Fox" && (it.pressureChart == null || it.maxPressurePsi == null) }
        assertTrue(foxWithoutChart.isEmpty(), "Fox forks lost their chart: ${foxWithoutChart.map { it.id }}")
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

    @Test
    fun `the Level RR 2026 is in the bike catalog on its stock parts`() {
        val bike = ComponentCatalog.bikeById("mondraker_level_rr_2026")
        assertNotNull(bike)
        bike!!
        assertEquals("Mondraker Level RR 2026", bike.displayName)
        assertEquals(BikeProfiles.levelRr.id, bike.profileId)
        assertEquals(BikeProfiles.levelRr.stockForkId, bike.stockFork.catalogId)
        assertEquals(BikeProfiles.levelRr.stockShockId, bike.stockShock.catalogId)
        assertEquals(mapOf("L" to 500.0, "XL" to 500.0), bike.springLbsBySize)
    }

    @Test
    fun `bikes from the 2026-09 research sample keep their verified stock parts`() {
        fun part(id: String, fork: Boolean) = ComponentCatalog.bikeById(id)!!.let { if (fork) it.stockFork else it.stockShock }
        assertEquals(StockPart(null, "Fox 36 Performance Elite", travelMm = 150), part("canyon_spectral_cf_8_2027", fork = true))
        assertEquals(StockPart(null, "Fox Float X Performance Elite", eyeToEyeMm = 210, strokeMm = 55.0, spring = SpringType.AIR), part("canyon_spectral_cf_8_2027", fork = false))
        assertEquals(StockPart(null, "RockShox ZEB Select+", travelMm = 180), part("canyon_torque_al_8_2027", fork = true))
        assertEquals(StockPart(null, "RockShox Vivid Select+", eyeToEyeMm = 250, strokeMm = 70.0, spring = SpringType.AIR), part("canyon_torque_al_8_2027", fork = false))
        assertEquals(StockPart(null, "Fox 38 Float Performance Grip", travelMm = 170), part("santacruz_megatower_90_2026", fork = true))
        assertEquals(StockPart(null, "Fox Float X Performance", eyeToEyeMm = 230, strokeMm = 65.0, spring = SpringType.AIR), part("santacruz_megatower_90_2026", fork = false))
        assertEquals(StockPart(null, "RockShox ZEB Select+", travelMm = 170), part("trek_slash_98_xt_di2_gen6", fork = true))
        assertEquals(StockPart(null, "RockShox Vivid Select+", eyeToEyeMm = 230, strokeMm = 65.0, spring = SpringType.AIR), part("trek_slash_98_xt_di2_gen6", fork = false))
        assertEquals(StockPart(null, "Fox 36 Float Factory GRIP X2", travelMm = 150), part("cube_stereo_c62_slt_2027", fork = true))
        assertEquals(StockPart(null, "Fox Float X Factory", eyeToEyeMm = 210, strokeMm = 55.0, spring = SpringType.AIR), part("cube_stereo_c62_slt_2027", fork = false))
        assertEquals(StockPart("ohlins_rxf38_m2_170", "Öhlins RXF38 m.2", travelMm = 170), part("yt_capra_mx_core3_cf", fork = true))
        assertEquals(StockPart(null, "Öhlins TTX22 m.2", eyeToEyeMm = 230, strokeMm = 65.0), part("yt_capra_mx_core3_cf", fork = false))

        val capra = ComponentCatalog.bikeById("yt_capra_mx_core3_cf")!!
        assertEquals(mapOf("S" to 343.0, "M" to 365.0, "L" to 388.0, "XL" to 411.0, "XXL" to 434.0), capra.springLbsBySize)
        assertEquals("YT Industries Capra MX Core 3 CF", capra.displayName)
        assertEquals("Trek Slash 9.8 XT Di2 Gen 6", ComponentCatalog.bikeById("trek_slash_98_xt_di2_gen6")!!.displayName)
        assertEquals(listOf("Canyon", "Cube", "Husqvarna", "Mondraker", "Santa Cruz", "Trek", "YT Industries"), ComponentCatalog.bikeMakers)
    }

    @Test
    fun `the Husqvarna Mountain Cross MC2 2023 has its stock parts, air shock and motor`() {
        val mc2 = ComponentCatalog.bikeById("husqvarna_mountain_cross_mc2_2023")!!
        assertEquals("Husqvarna Mountain Cross MC2 2023", mc2.displayName)
        assertEquals(StockPart(null, "RockShox 35 Gold RL", travelMm = 150), mc2.stockFork)
        assertEquals(StockPart(null, "RockShox Deluxe Select+", eyeToEyeMm = 230, strokeMm = 62.5, spring = SpringType.AIR), mc2.stockShock)
        assertEquals("Shimano EP6, DU-EP600", mc2.motor)
        assertEquals("husqvarna-mc2-2023", mc2.provenance.getValue("motor").source.id)
        assertEquals(listOf(2023), mc2.provenance.getValue("stockShock").source.modelYears)
        assertTrue(mc2.springLbsBySize.isEmpty(), "air shock: no spring per size")
    }

    @Test
    fun `every bike field names a source`() {
        val missing = ComponentCatalog.bikes.flatMap { b -> (listOf("stockFork", "stockShock") - b.provenance.keys).map { "${b.id}.$it" } }
        assertTrue(missing.isEmpty(), "Fields without source: $missing")
    }

    @Test
    fun `a bike pointing at an unknown fork is rejected`() {
        val json = """
            {"schemaVersion":1,"sources":{"s":{"title":"t","url":"u","modelYears":[2026],"retrieved":"2026-01-01"}},
             "forks":[],"shocks":[],
             "bikes":[{"id":"b","maker":"M","model":"X","modelYears":[2026],
               "stockFork":{"value":{"catalogId":"nope","name":"F"},"source":"s"},
               "stockShock":{"value":{"catalogId":null,"name":"S"},"source":"s"}}]}
        """.trimIndent()
        assertThrows<IllegalArgumentException> { CatalogJson.parse(json) }
    }

    @Test
    fun `a catalog without bikes still parses`() {
        val json = """{"schemaVersion":1,"sources":{},"forks":[],"shocks":[]}"""
        assertTrue(CatalogJson.parse(json).bikes.isEmpty())
    }
}
