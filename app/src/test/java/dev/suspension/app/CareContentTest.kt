package dev.suspension.app

import dev.suspension.app.care.CareContent
import dev.suspension.app.care.MaintCatalog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** Pflege tab: the static content follows the tagging rules, and nothing on it uses reserved colours. */
class CareContentTest {

    @Test
    fun `every card has a card tag or row tags, never both, never neither`() {
        val problems = mutableListOf<String>()
        for (section in CareContent.sections) {
            section.cards.forEachIndexed { index, card ->
                if (card.untagged || card.userData) return@forEachIndexed
                val where = "${section.id} card ${index + 1}"
                val rowTags = card.rows.count { it.tag != null }
                when {
                    card.cardTag != null && rowTags > 0 -> problems += "$where: card tag and row tags"
                    card.cardTag == null && card.rows.isEmpty() -> problems += "$where: no rows and no tag"
                    card.cardTag == null && rowTags != card.rows.size -> problems += "$where: untagged rows without card tag"
                }
            }
        }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun `section ids are unique and in chip order, calendar first`() {
        val ids = CareContent.sections.map { it.id }
        assertEquals(listOf("calendar", "after_ride", "products", "protection", "battery", "cells", "parts"), ids)
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `chip labels match the specification in German`() {
        val de = StringsXmlLoader.german
        val labels = CareContent.sections.map { section ->
            de.getValue(resourceName(section.chipLabelRes))
        }
        assertEquals(listOf("Kalender", "Nach der Fahrt", "Mittel", "Schutz & Lager", "Akku", "Batterien", "Teile"), labels)
    }

    @Test
    fun `task ids are unique and reset targets exist`() {
        val ids = MaintCatalog.tasks.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        MaintCatalog.tasks.flatMap { it.resetBy }.forEach { assertTrue(it in ids, "unknown resetBy $it") }
        assertEquals(17, ids.size)
    }

    @Test
    fun `selected verbatim texts are in the German strings`() {
        val de = StringsXmlLoader.german
        assertEquals("Herstellerangaben in diesem Abschnitt stammen von Bosch.", de.getValue("care_battery_intro"))
        assertEquals("Nicht laden. Erst abkühlen lassen.", de.getValue("care_battery_c2_r5_text"))
        assertEquals("Nichts fällig.", de.getValue("care_due_empty"))
        assertEquals("Wartung fällig", de.getValue("care_notif_both"))
        assertEquals("Hersteller", de.getValue("care_tag_hersteller"))
    }

    @Test
    fun `the Pflege UI never uses the reserved suspension colours`() {
        val dir = locate("src/main/java/dev/suspension/app/ui/care")
        val banned = Regex("""\.(comp|reb|spring)\b""")
        val hits = dir.walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            file.readLines().withIndex().filter { banned.containsMatchIn(it.value) }.map { "${file.name}:${it.index + 1}" }
        }.toList()
        assertTrue(hits.isEmpty(), "Reserved colours used: $hits")
    }

    /** The resource name of an id, via the generated R class. */
    private fun resourceName(id: Int): String =
        dev.suspension.app.R.string::class.java.fields.first { it.getInt(null) == id }.name

    private fun locate(relative: String): File {
        var dir: File? = File(".").absoluteFile
        repeat(4) {
            File(dir, relative).takeIf { it.exists() }?.let { return it }
            dir = dir?.parentFile
        }
        error("Could not locate $relative")
    }
}
