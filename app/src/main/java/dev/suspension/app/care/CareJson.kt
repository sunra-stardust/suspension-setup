package dev.suspension.app.care

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * `care.json`, versioned and edited in place so fields a newer release wrote survive our saves.
 * The calendar is kept per bike:
 *
 * ```
 * { "version": 1, "legacyBikeId": "<bike>",
 *   "settings": { remindersEnabled,                       ← global
 *                 purchaseDate, odometerKm, odometerUpdatedOn, kmPerHour,
 *                 reminderOverrides{}, notified{ id: {state, date} } },   ← the legacy bike
 *   "log": [ { id, date, km, taskIds[], note, createdAt } ],             ← the legacy bike
 *   "bikes": { "<bike id>": { "settings": {…}, "log": […] } } }          ← every other bike
 * ```
 * The legacy bike is the one whose data existed before the calendar was per bike (the garage's first
 * bike): its data stays at the top level, so an older release still reads exactly what it wrote.
 * Only keys are added, never renamed or dropped.
 */
object CareJson {
    const val VERSION = 1

    fun newDocument(): JSONObject = JSONObject().put("version", VERSION).put("settings", JSONObject()).put("log", JSONArray())

    fun legacyBikeId(doc: JSONObject): String? = doc.optString("legacyBikeId").takeIf { it.isNotBlank() }

    /** Records which bike owns the top-level data; set once and never moved. */
    fun claimLegacyBike(doc: JSONObject, bikeId: String) {
        if (legacyBikeId(doc) == null) doc.put("legacyBikeId", bikeId)
    }

    /** The object holding [bikeId]'s settings and log (created on write when [create]). */
    private fun node(doc: JSONObject, bikeId: String?, create: Boolean): JSONObject? {
        val legacy = legacyBikeId(doc)
        if (bikeId == null || legacy == null || bikeId == legacy) return doc
        val bikes = doc.optJSONObject("bikes") ?: if (create) JSONObject().also { doc.put("bikes", it) } else return null
        return bikes.optJSONObject(bikeId) ?: if (create) JSONObject().also { bikes.put(bikeId, it) } else null
    }

    /** Everything stored, per bike. */
    fun parseStore(doc: JSONObject): CareStore {
        val reminders = doc.optJSONObject("settings")?.optBoolean("remindersEnabled", false) ?: false
        val legacy = legacyBikeId(doc)
        val bikes = buildMap {
            if (legacy != null) put(legacy, parseNode(doc, reminders))
            doc.optJSONObject("bikes")?.let { b ->
                b.keys().forEach { id -> b.optJSONObject(id)?.let { put(id, parseNode(it, reminders)) } }
            }
        }
        return CareStore(remindersEnabled = reminders, legacyBikeId = legacy, bikes = bikes, unassigned = if (legacy == null) parseNode(doc, reminders) else null)
    }

    /** The legacy (top-level) data, as before the calendar was per bike. */
    fun parse(doc: JSONObject): CareData = parseStore(doc).let { store ->
        store.legacyBikeId?.let { store.bikes[it] } ?: store.unassigned ?: CareData(remindersEnabled = store.remindersEnabled)
    }

    fun parse(doc: JSONObject, bikeId: String): CareData = parseStore(doc).forBike(bikeId)

    private fun parseNode(n: JSONObject, remindersEnabled: Boolean): CareData {
        val s = n.optJSONObject("settings") ?: JSONObject()
        val overrides = s.optJSONObject("reminderOverrides")?.let { o ->
            o.keys().asSequence().filter { o.get(it) is Boolean }.associateWith { o.getBoolean(it) }
        } ?: emptyMap()
        val notified = s.optJSONObject("notified")?.let { o ->
            o.keys().asSequence().mapNotNull { id ->
                val x = o.optJSONObject(id) ?: return@mapNotNull null
                val state = runCatching { DueState.valueOf(x.getString("state")) }.getOrNull() ?: return@mapNotNull null
                val date = date(x.optString("date")) ?: return@mapNotNull null
                id to Notified(state, date)
            }.toMap()
        } ?: emptyMap()
        val log = n.optJSONArray("log")?.let { arr ->
            (0 until arr.length()).mapNotNull { entry(arr.optJSONObject(it)) }
        } ?: emptyList()
        return CareData(
            purchaseDate = date(s.optString("purchaseDate")),
            odometerKm = s.optInt("odometerKm", 0).coerceAtLeast(0),
            odometerUpdatedOn = date(s.optString("odometerUpdatedOn")),
            remindersEnabled = remindersEnabled,
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

    // --- Mutations on the document. [bikeId] null = the legacy (top-level) data. ---------------

    private fun settings(doc: JSONObject, bikeId: String?): JSONObject {
        val n = node(doc, bikeId, create = true)!!
        return n.optJSONObject("settings") ?: JSONObject().also { n.put("settings", it) }
    }

    private fun logArray(doc: JSONObject, bikeId: String?): JSONArray {
        val n = node(doc, bikeId, create = true)!!
        return n.optJSONArray("log") ?: JSONArray().also { n.put("log", it) }
    }

    fun setPurchaseDate(doc: JSONObject, date: LocalDate?, bikeId: String? = null) {
        settings(doc, bikeId).put("purchaseDate", date?.toString() ?: "")
    }

    fun setOdometer(doc: JSONObject, km: Int, today: LocalDate, bikeId: String? = null) {
        settings(doc, bikeId).put("odometerKm", km.coerceAtLeast(0)).put("odometerUpdatedOn", today.toString())
    }

    /** Global: one switch for all bikes, stored where older releases read it. */
    fun setReminders(doc: JSONObject, enabled: Boolean) {
        settings(doc, null).put("remindersEnabled", enabled)
    }

    fun setKmPerHour(doc: JSONObject, value: Int, bikeId: String? = null) {
        settings(doc, bikeId).put("kmPerHour", value.coerceIn(CareData.MIN_KM_PER_HOUR, CareData.MAX_KM_PER_HOUR))
    }

    fun setTaskReminder(doc: JSONObject, taskId: String, enabled: Boolean, bikeId: String? = null) {
        val s = settings(doc, bikeId)
        val o = s.optJSONObject("reminderOverrides") ?: JSONObject().also { s.put("reminderOverrides", it) }
        o.put(taskId, enabled)
    }

    fun setNotified(doc: JSONObject, notified: Map<String, Notified>, bikeId: String? = null) {
        val o = JSONObject()
        notified.forEach { (id, n) -> o.put(id, JSONObject().put("state", n.state.name).put("date", n.date.toString())) }
        settings(doc, bikeId).put("notified", o)
    }

    /**
     * Stores [entry] (replaces the entry with the same id). A higher km raises the odometer to it
     * ("Stand vom" = [today]); a lower one leaves the odometer alone. Returns true if it was raised.
     */
    fun saveEntry(doc: JSONObject, entry: LogEntry, today: LocalDate, bikeId: String? = null): Boolean {
        val arr = logArray(doc, bikeId)
        val index = (0 until arr.length()).firstOrNull { arr.optJSONObject(it)?.optString("id") == entry.id }
        if (index != null) entryJson(arr.getJSONObject(index), entry) else arr.put(entryJson(JSONObject(), entry))
        val current = settings(doc, bikeId).optInt("odometerKm", 0)
        val raised = entry.km > current
        if (raised) setOdometer(doc, entry.km, today, bikeId)
        return raised
    }

    fun deleteEntry(doc: JSONObject, id: String, bikeId: String? = null) {
        val n = node(doc, bikeId, create = true)!!
        val arr = n.optJSONArray("log") ?: return
        val kept = (0 until arr.length()).map { arr.get(it) }.filterNot { (it as? JSONObject)?.optString("id") == id }
        n.put("log", JSONArray(kept))
    }
}
