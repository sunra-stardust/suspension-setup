package dev.suspension.app

import dev.suspension.app.care.CareContent
import dev.suspension.app.care.CareText
import dev.suspension.app.care.MaintCatalog
import dev.suspension.app.care.MaintGroup
import dev.suspension.app.data.BikeProfiles
import dev.suspension.app.data.BikeSetup
import dev.suspension.app.data.BikeTraits
import dev.suspension.app.data.ComponentCatalog
import dev.suspension.app.data.CUSTOM_ID
import dev.suspension.app.data.DiagnoseData
import dev.suspension.app.data.ScenarioData
import dev.suspension.app.data.TextSpec
import dev.suspension.app.data.Trait
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Every text the app shows must fit the selected bike: no Bosch, motor, Mondraker parts, SRAM,
 * ONOFF or Fox statements on a bike that doesn't have those parts. Checked on what the screens
 * actually render (filtered content), in German and English.
 */
class BikeConsistencyTest {

    private val names: Map<Int, String> by lazy {
        R.string::class.java.fields.associate { it.getInt(null) to it.name }
    }

    private fun text(id: Int, language: Map<String, String> = StringsXmlLoader.german): String = language.getValue(names.getValue(id))

    /** Everything Pflege, the calendar and Diagnose show for [bike], as plain text. */
    private fun visibleTexts(bike: BikeTraits, language: Map<String, String>): List<Pair<String, String>> = buildList {
        fun add(where: String, id: Int) = add(where to text(id, language))
        for (section in CareContent.forBike(bike)) {
            add("chip ${section.id}", section.chipLabelRes)
            section.introRes?.let { add("${section.id} intro", it) }
            for (card in section.cards) {
                add("${section.id} card", card.titleRes)
                card.numbered.forEach { add("${section.id} item", it.res) }
                card.table?.rows?.forEach { row -> row.cells.forEach { add("${section.id} table", it) } }
                for (row in card.rows) {
                    add("${section.id} row", row.labelRes)
                    if (row.parts.isEmpty()) {
                        add("${section.id} row", row.textRes)
                    } else {
                        val joinLast = text(R.string.care_list_last, language)
                        add("${section.id} row" to CareText.listSentence(row.parts.map { text(it.res, language) }) { a, b -> joinLast.format(a, b) })
                    }
                }
            }
            section.closing.forEach { add("${section.id} closing", it.textRes) }
        }
        for (task in MaintCatalog.forBike(bike)) {
            add("task ${task.id}", task.nameRes(bike))
            add("task ${task.id}", task.intervalRes)
            task.noteRes(bike)?.let { add("task ${task.id}", it) }
        }
        for (group in MaintGroup.entries) {
            if (group.noteRes != null && bike.allows(group.noteCond)) add("group ${group.name}", group.noteRes!!)
            MaintCatalog.missingHints(group, bike).forEach { (res, arg) ->
                add("group ${group.name}" to text(res, language).let { if (arg != null) it.format(arg) else it })
            }
        }
        for (entry in DiagnoseData.forBike(bike)) {
            add("diagnose", entry.symptomResId)
            add("diagnose", entry.actionResId)
            add("diagnose", entry.explanationResId)
        }
        add(
            "odometer hint",
            when {
                bike.has(Trait.EBIKE_BOSCH) -> R.string.care_odo_hint
                bike.has(Trait.EBIKE) -> R.string.care_odo_hint_ebike
                else -> R.string.care_odo_hint_generic
            },
        )
        add("setup footer", if (bike.foxOnBike) R.string.setup_footer_note else R.string.setup_footer_note_generic)
        add("basics footer", if (bike.foxOnBike) R.string.basics_targets_footer else R.string.basics_targets_footer_generic)
    }

    private fun violations(bike: BikeTraits, banned: List<String>): List<String> =
        StringsXmlLoader.languages.flatMap { (lang, strings) ->
            visibleTexts(bike, strings).flatMap { (where, text) ->
                banned.filter { text.contains(it, ignoreCase = true) }.map { "[$lang] $where: \"$it\" in \"$text\"" }
            }
        }

    @Test
    fun `a bike the app knows nothing about shows no maker, motor or Level RR statements`() {
        val found = violations(BikeTraits.NONE, BRAND_WORDS + FOX_WORDS)
        assertTrue(found.isEmpty(), found.joinToString("\n"))
    }

    @Test
    fun `every catalog bike only shows statements about its own parts`() {
        val problems = mutableListOf<String>()
        for (model in ComponentCatalog.bikes) {
            val profile = model.profileId?.let(BikeProfiles::byId) ?: BikeProfiles.generic
            val (forkId, customFork) = BikeSetup.fork(model.stockFork)
            val (shockId, customShock) = BikeSetup.shock(model.stockShock, null)
            val fork = if (forkId == CUSTOM_ID) customFork!! else ComponentCatalog.forkById(forkId)!!
            val shock = if (shockId == CUSTOM_ID) customShock!! else ComponentCatalog.shockById(shockId)!!
            val bike = BikeTraits.of(profile, fork, shock, model)
            if (profile.id == BikeProfiles.levelRr.id) continue // the owner's bike: everything applies
            // Motor texts are right on any e-bike; Bosch statements only with Bosch.
            val brand = if (bike.has(Trait.EBIKE)) BRAND_WORDS - EBIKE_WORDS.toSet() else BRAND_WORDS
            val banned = brand + (if (bike.foxOnBike) emptyList() else FOX_WORDS) + (if (bike.has(Trait.SHOCK_AIR)) COIL_WORDS else emptyList())
            problems += violations(bike, banned).map { "${model.displayName}: $it" }
        }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun `the Level RR with its stock parts still shows the full specification content`() {
        val bike = levelRr()
        assertEquals(MaintCatalog.tasks.map { it.id }, MaintCatalog.forBike(bike).map { it.id })
        assertEquals(CareContent.sections.map { it.id }, CareContent.forBike(bike).map { it.id })
        // Verbatim as specified: original texts, never the neutral variants.
        val shown = visibleTexts(bike, StringsXmlLoader.german).map { it.second }
        assertTrue(text(R.string.care_after_ride_c1_r1_text) in shown)
        assertFalse(text(R.string.care_after_ride_c1_r1_text_generic) in shown)
        assertTrue(text(R.string.care_protection_c2_r3_text) in shown, "list sentence must equal the original")
        assertTrue(text(R.string.care_task_play_check_name) in shown)
        assertTrue(text(R.string.care_task_shock_overhaul_note) in shown)
        assertTrue(text(R.string.diag_explanation_7) in shown)
        assertTrue(text(R.string.diag_explanation_14) in shown)
        assertEquals(DiagnoseData.entries.count { it.cond.isEmpty() } + 6, DiagnoseData.forBike(bike).size)
    }

    @Test
    fun `swapping the fork on the Level RR drops the Fox fork intervals and says so`() {
        val profile = BikeProfiles.levelRr
        val ohlins = ComponentCatalog.forkById("ohlins_rxf38_m2_170")!!
        val bike = BikeTraits.of(profile, ohlins, ComponentCatalog.shockById(profile.stockShockId)!!)
        val ids = MaintCatalog.forBike(bike).map { it.id }
        assertFalse("fork_lower" in ids)
        assertFalse("fork_overhaul" in ids)
        assertTrue("shock_overhaul" in ids)
        assertEquals(listOf(R.string.care_missing_fork to ohlins.displayName), MaintCatalog.missingHints(MaintGroup.SUSPENSION, bike))
        // The fork's diagnose advice no longer quotes Fox; the shock's still does.
        val explanations = DiagnoseData.forBike(bike).map { it.explanationResId }
        assertTrue(R.string.diag_explanation_11_generic in explanations)
        assertTrue(R.string.diag_explanation_12 in explanations)
    }

    @Test
    fun `a non-Fox shock loses the DHX2 note and the Fox shock interval`() {
        val profile = BikeProfiles.levelRr
        val rockshox = ComponentCatalog.shockById("rockshox_vivid_coil")!!
        val bike = BikeTraits.of(profile, ComponentCatalog.forkById(profile.stockForkId)!!, rockshox)
        assertFalse(bike.has(Trait.SHOCK_FOX_DHX2))
        assertFalse("shock_overhaul" in MaintCatalog.forBike(bike).map { it.id })
        assertTrue(bike.has(Trait.SHOCK_COIL))
    }

    @Test
    fun `without a Bosch drive the battery chip disappears and the list sentence drops the contacts`() {
        val sections = CareContent.forBike(BikeTraits.NONE).map { it.id }
        assertFalse("battery" in sections)
        assertFalse("cells" in sections)
        assertFalse("parts" in sections)
        assertTrue("after_ride" in sections)
        val row = visibleTexts(BikeTraits.NONE, StringsXmlLoader.german).first { it.second.startsWith("Bremsscheiben") }
        assertEquals("Bremsscheiben (Flugrost), Kette und Lager.", row.second)
    }

    @Test
    fun `cards keep the tagging rule and never render empty for any bike`() {
        for (bike in listOf(BikeTraits.NONE, levelRr(), BikeTraits(setOf(Trait.SHIFTING_SRAM_AXS)), BikeTraits(setOf(Trait.EBIKE_BOSCH)))) {
            for (section in CareContent.forBike(bike)) {
                for (card in section.cards) {
                    assertFalse(card.isEmpty, "${section.id}: empty card")
                    if (!card.untagged && !card.userData) {
                        assertTrue(card.cardTag != null || card.rows.all { it.tag != null }, "${section.id}: untagged row")
                        assertFalse(card.cardTag != null && card.rows.any { it.tag != null }, "${section.id}: both tags")
                    }
                }
            }
        }
    }

    @Test
    fun `the setup hint for the shock's low-speed compression only mentions the motor on an e-bike`() {
        fun lscHint(profile: dev.suspension.app.data.BikeProfile): Int {
            val shock = ComponentCatalog.shockById(profile.stockShockId)!!
            val row = ScenarioData.buildShockGroup(shock, 90.0, 20, profile).rows.first { it.id == "s_lsc" }
            return (row.hint as TextSpec.Res).id
        }
        assertEquals(R.string.hint_s_lsc, lscHint(BikeProfiles.levelRr))
        assertEquals(R.string.hint_s_lsc_generic, lscHint(BikeProfiles.generic))
    }

    @Test
    fun `the Husqvarna MC2 is an e-bike without Bosch, with an air shock and a pressure row`() {
        val model = ComponentCatalog.bikeById("husqvarna_mountain_cross_mc2_2023")!!
        val profile = BikeProfiles.generic
        val fork = BikeSetup.fork(model.stockFork).second!!
        val shock = BikeSetup.shock(model.stockShock, null).second!!
        val bike = BikeTraits.of(profile, fork, shock, model)
        assertTrue(bike.has(Trait.EBIKE))
        assertFalse(bike.has(Trait.EBIKE_BOSCH))
        assertTrue(bike.has(Trait.SHOCK_AIR))
        assertFalse(bike.has(Trait.SHOCK_COIL))
        assertEquals(62.5, shock.strokeMm)

        // Pflege: motor texts yes, Bosch battery section no; motor in the play check, no Gen5 remark.
        val sections = CareContent.forBike(bike).map { it.id }
        assertFalse("battery" in sections)
        val play = MaintCatalog.byId.getValue("play_check")
        assertEquals(R.string.care_task_play_check_name, play.nameRes(bike))
        assertEquals(R.string.care_task_play_check_note_ebike, play.noteRes(bike))
        assertFalse("first_inspection" in MaintCatalog.forBike(bike).map { it.id })

        // Setup: pressure instead of spring rate and preload; start ≈ rider weight in lbs.
        val rows = ScenarioData.buildShockGroup(shock, 90.0, 20, profile, ebike = true).rows
        assertTrue(rows.none { it.id == "s_rate" || it.id == "s_pre" })
        val psi = rows.first { it.id == "s_psi" } as dev.suspension.app.data.RowSpec.Stepper
        assertEquals(200.0, psi.start)
        assertEquals(R.string.hint_s_lsc, (rows.first { it.id == "s_lsc" }.hint as TextSpec.Res).id)

        // Diagnose: air advice instead of springs.
        val explanations = DiagnoseData.forBike(bike).map { it.explanationResId }
        assertTrue(R.string.diag_explanation_12_air in explanations)
        assertTrue(R.string.diag_explanation_13_air in explanations)
        assertFalse(R.string.diag_explanation_12_generic in explanations)
    }

    @Test
    fun `a bike created before air shocks were known reads its prefilled shock as air`() {
        val model = ComponentCatalog.bikeById("canyon_torque_al_8_2027")!!
        val storedAsCoil = BikeSetup.shock(model.stockShock, null).second!!.copy(spring = dev.suspension.app.data.SpringType.COIL, customSpringLbs = 500.0)
        val bike = dev.suspension.app.data.Bike(
            id = "b", name = null, profileId = BikeProfiles.GENERIC_ID, forkId = "fox38_gripx2", shockId = CUSTOM_ID,
            customFork = null, customShock = storedAsCoil, springLbs = null, edits = emptyMap(), catalogBikeId = model.id,
        )
        val shock = dev.suspension.app.data.BikeParts.shock(bike, BikeProfiles.generic)
        assertTrue(shock.isAir)
        // Once the rider confirmed his own shock, his choice stands.
        val confirmed = bike.copy(customShock = storedAsCoil.copy(needsCheck = false))
        assertFalse(dev.suspension.app.data.BikeParts.shock(confirmed, BikeProfiles.generic).isAir)
    }

    private fun levelRr(): BikeTraits {
        val p = BikeProfiles.levelRr
        return BikeTraits.of(p, ComponentCatalog.forkById(p.stockForkId)!!, ComponentCatalog.shockById(p.stockShockId)!!)
    }

    private companion object {
        val BRAND_WORDS = listOf(
            "Mondraker", "Bosch", "SRAM", "AXS", "Maven", "ONOFF", "OnOff", "Pija", "Gen5", "Motor", "E-MTB", "E-Bike",
            "Akku", "battery", "Ladebuchse", "charging port", "Flow-App", "Flow app", "Flip-Chip", "flip chip", "DHX2",
        )
        val FOX_WORDS = listOf("Fox", "ridefox")
        /** Words that are right on any e-bike, not only a Bosch one. */
        val EBIKE_WORDS = listOf("Motor", "E-MTB", "E-Bike", "Ladebuchse", "charging port")
        /** Coil-only wording that must not reach an air shock. */
        val COIL_WORDS = listOf("Stahlfeder", "steel spring", "Härtere Feder", "Nächsthärtere Feder", "stiffer spring", "Vorspannung")
    }
}
