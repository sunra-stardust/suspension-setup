package dev.suspension.app.care

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * `care.json`, versioned and edited in place so fields a newer release wrote survive our saves:
 *
 * ```
 * { "version": 1, "settings": { purchaseDate, odometerKm, odometerUpdatedOn, remindersEnabled,
 *                                kmPerHour, reminderOverrides{}, notified{ id: {state, date} } },
 *   "log": [ { id, date, km, taskIds[], note, createdAt } ] }
 * ```
 * Only keys are added, never renamed or dropped (an older release must read what a newer one wrote).
 */
object CareJson {
    const val VERSION = 1

    fun newDocument(): JSONObject = JSONObject().put("version", VERSION).put("settings", JSONObject()).put("log", JSONArray())

    fun parse(doc: JSONObject): CareData {
        val s = doc.optJSONObject("settings") ?: JSONObject()
        val overrides = s.optJSONObject("reminderOverrides")?.let { o ->
            o.keys().asSequence().filter { o.get(it) is Boolean }.associateWith { o.getBoolean(it) }
        } ?: emptyMap()
        val notified = s.optJSONObject("notified")?.let { o ->
            o.keys().asSequence().mapNotNull { id ->
                val n = o.optJSONObject(id) ?: return@mapNotNull null
                val state = runCatching { DueState.valueOf(n.getString("state")) }.getOrNull() ?: return@mapNotNull null
                val date = date(n.optString("date")) ?: return@mapNotNull null
                id to Notified(state, date)
            }.toMap()
        } ?: emptyMap()
        val log = doc.optJSONArray("log")?.let { arr ->
            (0 until arr.length()).mapNotNull { entry(arr.optJSONObject(it)) }
        } ?: emptyList()
        return CareData(
            purchaseDate = date(s.optString("purchaseDate")),
            odometerKm = s.optInt("odometerKm", 0).coerceAtLeast(0),
            odometerUpdatedOn = date(s.optString("odometerUpdatedOn")),
            remindersEnabled = s.optBoolean("remindersEnabled", false),
            kmPerHour = s.optInt("kmPerHour", CareData.DEFAULT_KM_PER_HOUR)
                .coerceIn(CareData.MIN_KM_PER_HOUR, CareData.MAX_KM_PER_HOUR),
            reminderOverrides = overrides,
            notified = notified,
            log = log,
        )
    }

    private fun date(text: String?): LocalDate? = text?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

    private fun entry(o: JSONObject?): LogEntry? {
        o ?: return null
        val id = o.optString("id").takeIf { it.isNotBlank() } ?: return null
        val date = date(o.optString("date")) ?: return null
        val ids = o.optJSONArray("taskIds")?.let { a -> (0 until a.length()).map { a.optString(it) } } ?: emptyList()
        return LogEntry(id, date, o.optInt("km", 0), ids, o.optString("note", ""), o.optLong("createdAt", 0L))
    }

    private fun entryJson(into: JSONObject, e: LogEntry): JSONObject = into
        .put("id", e.id).put("date", e.date.toString()).put("km", e.km)
        .put("taskIds", JSONArray(e.taskIds)).put("note", e.note).put("createdAt", e.createdAt)

    // --- Mutations on the document ------------------------------------------------------------

    private fun settings(doc: JSONObject): JSONObject = doc.optJSONObject("settings") ?: JSONObject().also { doc.put("settings", it) }

    private fun logArray(doc: JSONObject): JSONArray = doc.optJSONArray("log") ?: JSONArray().also { doc.put("log", it) }

    fun setPurchaseDate(doc: JSONObject, date: LocalDate?) {
        settings(doc).put("purchaseDate", date?.toString() ?: "")
    }

    fun setOdometer(doc: JSONObject, km: Int, today: LocalDate) {
        settings(doc).put("odometerKm", km.coerceAtLeast(0)).put("odometerUpdatedOn", today.toString())
    }

    fun setReminders(doc: JSONObject, enabled: Boolean) {
        settings(doc).put("remindersEnabled", enabled)
    }

    fun setKmPerHour(doc: JSONObject, value: Int) {
        settings(doc).put("kmPerHour", value.coerceIn(CareData.MIN_KM_PER_HOUR, CareData.MAX_KM_PER_HOUR))
    }

    fun setTaskReminder(doc: JSONObject, taskId: String, enabled: Boolean) {
        val s = settings(doc)
        val o = s.optJSONObject("reminderOverrides") ?: JSONObject().also { s.put("reminderOverrides", it) }
        o.put(taskId, enabled)
    }

    fun setNotified(doc: JSONObject, notified: Map<String, Notified>) {
        val o = JSONObject()
        notified.forEach { (id, n) -> o.put(id, JSONObject().put("state", n.state.name).put("date", n.date.toString())) }
        settings(doc).put("notified", o)
    }

    /**
     * Stores [entry] (replaces the entry with the same id). A higher km raises the odometer to it
     * ("Stand vom" = [today]); a lower one leaves the odometer alone. Returns true if it was raised.
     */
    fun saveEntry(doc: JSONObject, entry: LogEntry, today: LocalDate): Boolean {
        val arr = logArray(doc)
        val index = (0 until arr.length()).firstOrNull { arr.optJSONObject(it)?.optString("id") == entry.id }
        if (index != null) entryJson(arr.getJSONObject(index), entry) else arr.put(entryJson(JSONObject(), entry))
        val raised = entry.km > parse(doc).odometerKm
        if (raised) setOdometer(doc, entry.km, today)
        return raised
    }

    fun deleteEntry(doc: JSONObject, id: String) {
        val arr = logArray(doc)
        val kept = (0 until arr.length()).map { arr.get(it) }.filterNot { (it as? JSONObject)?.optString("id") == id }
        doc.put("log", JSONArray(kept))
    }
}
