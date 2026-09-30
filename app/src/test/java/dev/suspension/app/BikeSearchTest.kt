package dev.suspension.app

import dev.suspension.app.data.BikeModel
import dev.suspension.app.data.StockPart
import dev.suspension.app.data.ComponentCatalog
import dev.suspension.app.ui.groupByMaker
import dev.suspension.app.ui.searchBikes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Phase 5.2/5.3: search and order in the bike and component pickers. */
class BikeSearchTest {

    private fun bike(maker: String, model: String, trim: String, year: Int) = BikeModel(
        id = "${maker}_${model}_${trim}_$year".lowercase(), maker = maker, model = model, trim = trim,
        modelYears = listOf(year), profileId = null,
        stockFork = StockPart(null, "F"), stockShock = StockPart(null, "S"), springLbsBySize = emptyMap(),
    )

    private val bikes = listOf(
        bike("Mondraker", "Level", "RR", 2025),
        bike("Cube", "Stereo", "ONE77", 2026),
        bike("Mondraker", "Level", "RR", 2026),
        bike("Mondraker", "Dune", "R", 2026),
    )

    @Test
    fun `search matches maker, model and trim, ignoring case`() {
        assertEquals(listOf("Mondraker Level RR 2026", "Mondraker Level RR 2025"), searchBikes(bikes, " level rr ").map { it.displayName })
        assertEquals(listOf("Cube Stereo ONE77 2026"), searchBikes(bikes, "CUBE").map { it.displayName })
        assertEquals(emptyList<String>(), searchBikes(bikes, "Canyon").map { it.displayName })
    }

    @Test
    fun `results are sorted by maker and model, newest year first`() {
        assertEquals(
            listOf("Cube Stereo ONE77 2026", "Mondraker Dune R 2026", "Mondraker Level RR 2026", "Mondraker Level RR 2025"),
            searchBikes(bikes, "").map { it.displayName },
        )
    }

    @Test
    fun `component search groups by maker and matches maker or model`() {
        val all = groupByMaker(ComponentCatalog.shocks, "", { it.maker }, { it.displayName })
        assertEquals(listOf("Fox", "RockShox"), all.map { it.first })
        assertEquals(ComponentCatalog.shocks.size, all.sumOf { it.second.size })

        val rockShox = groupByMaker(ComponentCatalog.shocks, "rockshox", { it.maker }, { it.displayName })
        assertEquals(listOf("RockShox"), rockShox.map { it.first })
        assertEquals(listOf("Fox DHX2 Factory"), groupByMaker(ComponentCatalog.shocks, "dhx2 factory", { it.maker }, { it.displayName }).flatMap { g -> g.second.map { it.displayName } })
        assertEquals(emptyList<Any>(), groupByMaker(ComponentCatalog.shocks, "Öhlins", { it.maker }, { it.displayName }))
    }
}
