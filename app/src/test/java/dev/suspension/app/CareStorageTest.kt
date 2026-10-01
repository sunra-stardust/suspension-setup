package dev.suspension.app

import dev.suspension.app.care.CareData
import dev.suspension.app.care.CareFormat
import dev.suspension.app.care.CareJson
import dev.suspension.app.care.CareReminders
import dev.suspension.app.care.DueState
import dev.suspension.app.care.EntryValidation
import dev.suspension.app.care.LogEntry
import dev.suspension.app.care.Notified
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class CareStorageTest {

    private val today = LocalDate.of(2026, 11, 15)

    private fun entry(id: String, km: Int, vararg tasks: String, date: LocalDate = today, note: String = "") =
        LogEntry(id, date, km, tasks.toList(), note, 1L)

    @Test
    fun `an empty document parses to defaults`() {
        val data = CareJson.parse(CareJson.newDocument())
        assertEquals(CareData(), data)
        assertEquals(15, data.kmPerHour)
    }

    @Test
    fun `settings and log round trip`() {
        val doc = CareJson.newDocument()
        CareJson.setPurchaseDate(doc, LocalDate.of(2026, 10, 12))
        CareJson.setOdometer(doc, 320, today)
        CareJson.setReminders(doc, true)
        CareJson.setKmPerHour(doc, 18)
        CareJson.setTaskReminder(doc, "pads", true)
        CareJson.setNotified(doc, mapOf("screws" to Notified(DueState.OVERDUE, today)))
        CareJson.saveEntry(doc, entry("a", 320, "screws", note = "Mit Drehmoment"), today)

        val data = CareJson.parse(JSONObject(doc.toString()))
        assertEquals(LocalDate.of(2026, 10, 12), data.purchaseDate)
        assertEquals(320, data.odometerKm)
        assertEquals(today, data.odometerUpdatedOn)
        assertTrue(data.remindersEnabled)
        assertEquals(18, data.kmPerHour)
        assertEquals(mapOf("pads" to true), data.reminderOverrides)
        assertEquals(Notified(DueState.OVERDUE, today), data.notified["screws"])
        assertEquals(listOf(entry("a", 320, "screws", note = "Mit Drehmoment")), data.log)
    }

    @Test
    fun `clearing the purchase date`() {
        val doc = CareJson.newDocument()
        CareJson.setPurchaseDate(doc, today)
        CareJson.setPurchaseDate(doc, null)
        assertNull(CareJson.parse(doc).purchaseDate)
    }

    @Test
    fun `an entry with a higher km raises the odometer, a lower one does not`() {
        val doc = CareJson.newDocument()
        CareJson.setOdometer(doc, 320, LocalDate.of(2026, 10, 12))

        assertTrue(CareJson.saveEntry(doc, entry("a", 1240, "headset"), today))
        assertEquals(1240, CareJson.parse(doc).odometerKm)
        assertEquals(today, CareJson.parse(doc).odometerUpdatedOn)

        assertFalse(CareJson.saveEntry(doc, entry("b", 300, "headset", date = LocalDate.of(2026, 10, 1)), today))
        assertEquals(1240, CareJson.parse(doc).odometerKm)
        assertEquals(2, CareJson.parse(doc).log.size)
    }

    @Test
    fun `saving an entry again replaces it, deleting removes only it`() {
        val doc = CareJson.newDocument()
        CareJson.saveEntry(doc, entry("a", 100, "screws"), today)
        CareJson.saveEntry(doc, entry("b", 200, "headset"), today)
        CareJson.saveEntry(doc, entry("a", 100, "screws", note = "geändert"), today)
        assertEquals(listOf("geändert", ""), CareJson.parse(doc).log.map { it.note })

        CareJson.deleteEntry(doc, "a")
        assertEquals(listOf("b"), CareJson.parse(doc).log.map { it.id })
    }

    @Test
    fun `unknown fields written by a newer release survive our saves`() {
        val doc = JSONObject(
            """{"version":2,"future":{"x":1},"settings":{"purchaseDate":"2026-10-01","newKey":"keep"},
               "log":[{"id":"a","date":"2026-11-01","km":10,"taskIds":["gone_task"],"note":"alt","createdAt":1,"extra":true}]}""",
        )
        CareJson.setOdometer(doc, 50, today)
        CareJson.saveEntry(doc, entry("a", 10, "gone_task", date = LocalDate.of(2026, 11, 1), note = "neu"), today)
        assertEquals(1, doc.getJSONObject("future").getInt("x"))
        assertEquals("keep", doc.getJSONObject("settings").getString("newKey"))
        assertTrue(doc.getJSONArray("log").getJSONObject(0).getBoolean("extra"))
        assertEquals(2, doc.getInt("version"))
        // Unknown task ids stay in the entry.
        assertEquals(listOf("gone_task"), CareJson.parse(doc).log.single().taskIds)
    }

    @Test
    fun `damaged entries are skipped in the view but stay in the document`() {
        val doc = JSONObject("""{"log":[{"id":"a","date":"kaputt","km":1},{"id":"b","date":"2026-11-01","km":2}]}""")
        assertEquals(listOf("b"), CareJson.parse(doc).log.map { it.id })
        assertEquals(2, doc.getJSONArray("log").length())
    }

    @Test
    fun `km per hour stays within its range`() {
        val doc = CareJson.newDocument()
        CareJson.setKmPerHour(doc, 99)
        assertEquals(25, CareJson.parse(doc).kmPerHour)
        CareJson.setKmPerHour(doc, 1)
        assertEquals(8, CareJson.parse(doc).kmPerHour)
    }

    @Test
    fun `entry validation`() {
        fun v(date: LocalDate = today, km: String = "320", tasks: List<String> = listOf("screws"), note: String = "") =
            EntryValidation.validate(date, km, tasks, note, today)
        assertFalse(v().any)
        assertTrue(v(date = today.plusDays(1)).dateInFuture)
        assertTrue(v(km = "").kmMissing)
        assertTrue(v(km = "abc").kmMissing)
        assertTrue(v(km = "-5").kmMissing)
        assertEquals(0, EntryValidation.parseKm("0"))
        assertTrue(v(tasks = emptyList(), note = "  ").nothingDone)
        assertFalse(v(tasks = emptyList(), note = "Gabel Service").nothingDone)
    }

    @Test
    fun `numbers and dates are written the German way`() {
        assertEquals("1.240", CareFormat.km(1240))
        assertEquals("320", CareFormat.km(320))
        assertEquals("12.03.2027", CareFormat.date(LocalDate.of(2027, 3, 12)))
    }

    @Test
    fun `the daily check first runs at the next 09 00`() {
        val zone = ZoneId.of("Europe/Berlin")
        val morning = ZonedDateTime.of(2026, 11, 15, 7, 30, 0, 0, zone)
        assertEquals(ZonedDateTime.of(2026, 11, 15, 9, 0, 0, 0, zone), CareReminders.nextRun(morning))
        val afternoon = ZonedDateTime.of(2026, 11, 15, 9, 0, 0, 0, zone)
        assertEquals(ZonedDateTime.of(2026, 11, 16, 9, 0, 0, 0, zone), CareReminders.nextRun(afternoon))
    }

    @Test
    fun `remaining texts have singular forms in both languages`() {
        val de = StringsXmlLoader.german
        val en = StringsXmlLoader.english
        assertEquals("in 1 Tag", de.getValue("care_remaining_in_days#one").format(1))
        assertEquals("in 41 Tagen", de.getValue("care_remaining_in_days#other").format(41))
        assertEquals("seit 1 Tag", de.getValue("care_remaining_since_days#one").format(1))
        assertEquals("seit 12 Tagen", de.getValue("care_remaining_since_days#other").format(12))
        assertEquals("in 4 Monaten", de.getValue("care_remaining_in_months#other").format(4))
        assertEquals("in 230 km", de.getValue("care_remaining_in_km").format("230"))
        assertEquals("seit 80 km", de.getValue("care_remaining_since_km").format("80"))
        assertEquals("in 1 day", en.getValue("care_remaining_in_days#one").format(1))
        assertEquals("Fällig bei 1.240 km oder am 12.03.2027", de.getValue("care_due_at_km_or_date").format("1.240", "12.03.2027"))
        assertEquals("Zuletzt: 12.03.2027 bei 1.240 km", de.getValue("care_last_done").format("12.03.2027", "1.240"))
        assertEquals("Stand vom 12.10.2026 · älter als 30 Tage", de.getValue("care_odometer_asof_old").format("12.10.2026"))
    }
}
