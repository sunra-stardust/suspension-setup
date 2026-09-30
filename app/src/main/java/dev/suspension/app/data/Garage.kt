package dev.suspension.app.data

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.round

/**
 * A riding situation. Built-ins map to the former [Scenario] tabs (and to the legacy storage
 * keys); custom ones (phase 3.3) have a rider-given name. Shared by all bikes.
 */
data class Vorlage(val id: String, val builtIn: Scenario?, val name: String?)

/** The rider's own change to one row, relative to the row's starting value (owner decision 2026-09-30). */
sealed class Edit {
    /** Stepper rows: shown value = start + delta, clamped to the row's range. */
    data class Delta(val delta: Double) : Edit()

    /** Toggle rows: the chosen option (stored as-is, never translated). */
    data class Choice(val option: String) : Edit()
}

data class Bike(
    val id: String,
    /** Rider-given name; null = the profile's name. */
    val name: String?,
    val profileId: String,
    val forkId: String,
    val shockId: String,
    /** The rider's own fork/shock (used when [forkId]/[shockId] is [CUSTOM_ID]); null = never set. */
    val customFork: ForkModel?,
    val customShock: ShockModel?,
    /** Installed spring when the frame profile doesn't define one (bikes created in the app). */
    val springLbs: Double?,
    /** Vorlage id → row id → edit. Absent = the row shows its starting value. */
    val edits: Map<String, Map<String, Edit>>,
) {
    fun editsFor(vorlageId: String): Map<String, Edit> = edits[vorlageId].orEmpty()
}

data class Garage(
    val weightKg: Double,
    val tempC: Int,
    val vorlagen: List<Vorlage>,
    val bikes: List<Bike>,
    val selectedBikeId: String,
    val selectedVorlageId: String,
) {
    val selectedBike: Bike get() = bikes.firstOrNull { it.id == selectedBikeId } ?: bikes.first()
    val selectedVorlage: Vorlage get() = vorlagen.firstOrNull { it.id == selectedVorlageId } ?: vorlagen.first()
}

/** Shown value of a row given the rider's edit — the one place relative edits are resolved. */
object RowValues {
    fun stepper(row: RowSpec.Stepper, edit: Edit?): Double {
        val delta = (edit as? Edit.Delta)?.delta ?: 0.0
        return clamp(row, row.start + delta)
    }

    fun toggle(row: RowSpec.Toggle, edit: Edit?): String =
        (edit as? Edit.Choice)?.option?.takeIf { it in row.options } ?: row.start

    fun clamp(row: RowSpec.Stepper, value: Double): Double =
        round(value.coerceIn(0.0, row.max ?: Double.MAX_VALUE) * 100) / 100.0

    /** The edit that makes [row] show [value]; null when that is the starting value. */
    fun editFor(row: RowSpec.Stepper, value: Double): Edit? {
        val delta = round((clamp(row, value) - row.start) * 100) / 100.0
        return if (abs(delta) < 1e-9) null else Edit.Delta(delta)
    }

    fun editFor(row: RowSpec.Toggle, option: String): Edit? = if (option == row.start) null else Edit.Choice(option)
}

/**
 * `garage.json` ↔ [Garage]. Reading is lenient (unknown fields are ignored, so a newer release's
 * file still opens); writes go through [GarageDoc] mutations on the JSON itself, so fields this
 * release doesn't know survive a save — an older release never destroys a newer one's data.
 */
object GarageJson {
    const val SCHEMA_VERSION = 1

    fun parse(doc: JSONObject): Garage {
        val rider = doc.optJSONObject("rider") ?: JSONObject()
        val vorlagenJson = doc.optJSONArray("vorlagen")
        val vorlagen = (0 until (vorlagenJson?.length() ?: 0)).mapNotNull { i ->
            val o = vorlagenJson!!.getJSONObject(i)
            val builtIn = o.optStringOrNull("builtIn")?.let { tag -> Scenario.entries.firstOrNull { it.tag == tag } }
            if (builtIn == null && o.optStringOrNull("name") == null) null
            else Vorlage(o.getString("id"), builtIn, o.optStringOrNull("name"))
        }.ifEmpty { BUILT_IN_VORLAGEN }
        val bikesJson = doc.getJSONArray("bikes")
        val bikes = (0 until bikesJson.length()).map { parseBike(bikesJson.getJSONObject(it)) }
        require(bikes.isNotEmpty()) { "garage.json has no bike" }
        val selection = doc.optJSONObject("selection") ?: JSONObject()
        return Garage(
            weightKg = rider.optDouble("weightKg", DEFAULT_WEIGHT_KG),
            tempC = rider.optInt("tempC", DEFAULT_TEMP_C),
            vorlagen = vorlagen,
            bikes = bikes,
            selectedBikeId = selection.optStringOrNull("bikeId") ?: bikes.first().id,
            selectedVorlageId = selection.optStringOrNull("vorlageId") ?: vorlagen.first().id,
        )
    }

    private fun parseBike(o: JSONObject): Bike {
        val values = o.optJSONObject("values") ?: JSONObject()
        val edits = values.keys().asSequence().associateWith { vorlageId ->
            val rows = values.getJSONObject(vorlageId)
            rows.keys().asSequence().mapNotNull { rowId ->
                val e = rows.getJSONObject(rowId)
                val edit = when {
                    e.has("delta") -> Edit.Delta(e.getDouble("delta"))
                    e.has("choice") -> Edit.Choice(e.getString("choice"))
                    else -> null
                }
                edit?.let { rowId to it }
            }.toMap()
        }
        return Bike(
            id = o.getString("id"),
            name = o.optStringOrNull("name"),
            profileId = o.getString("profile"),
            forkId = o.getString("forkId"),
            shockId = o.getString("shockId"),
            customFork = o.optJSONObject("customFork")?.let(::parseCustomFork),
            customShock = o.optJSONObject("customShock")?.let(::parseCustomShock),
            springLbs = if (o.has("springLbs") && !o.isNull("springLbs")) o.getDouble("springLbs") else null,
            edits = edits,
        )
    }

    fun customForkJson(f: ForkModel): JSONObject = JSONObject()
        .put("name", f.displayName)
        .put("travelMm", f.travelMm)
        .put("lscMax", f.lscMax)
        .put("hscMax", f.hscMax ?: JSONObject.NULL)
        .put("reboundSplit", f.reboundMode == ReboundMode.SPLIT)
        .put("reboundMax", f.reboundMax)
        .put("hsrMax", f.hsrMax ?: JSONObject.NULL)
        .put("baselinePsi", f.baselinePsi)

    private fun parseCustomFork(o: JSONObject) = ForkModel(
        id = CUSTOM_ID,
        displayName = o.optString("name", ""),
        travelMm = o.getInt("travelMm"),
        lscMax = o.getInt("lscMax"),
        hscMax = o.optIntOrNull("hscMax"),
        reboundMode = if (o.optBoolean("reboundSplit", true)) ReboundMode.SPLIT else ReboundMode.SINGLE,
        reboundMax = o.getInt("reboundMax"),
        hsrMax = o.optIntOrNull("hsrMax"),
        pressureChart = null,
        baselinePsi = o.optDouble("baselinePsi", 100.0),
        maxPressurePsi = null,
        spacersStock = null,
        spacersMax = null,
    )

    fun customShockJson(s: ShockModel): JSONObject = JSONObject()
        .put("name", s.displayName)
        .put("strokeMm", s.strokeMm)
        .put("eyeToEyeMm", s.eyeToEyeMm)
        .put("lscMax", s.lscMax)
        .put("hscMax", s.hscMax ?: JSONObject.NULL)
        .put("reboundSplit", s.reboundMode == ReboundMode.SPLIT)
        .put("reboundMax", s.reboundMax)
        .put("hsrMax", s.hsrMax ?: JSONObject.NULL)
        .put("climbLever", s.hasClimbLever)
        .put("springLbs", s.customSpringLbs ?: JSONObject.NULL)

    private fun parseCustomShock(o: JSONObject) = ShockModel(
        id = CUSTOM_ID,
        displayName = o.optString("name", ""),
        strokeMm = o.getInt("strokeMm"),
        eyeToEyeMm = o.getInt("eyeToEyeMm"),
        lscMax = o.getInt("lscMax"),
        hscMax = o.optIntOrNull("hscMax"),
        reboundMode = if (o.optBoolean("reboundSplit", true)) ReboundMode.SPLIT else ReboundMode.SINGLE,
        reboundMax = o.getInt("reboundMax"),
        hsrMax = o.optIntOrNull("hsrMax"),
        hasClimbLever = o.optBoolean("climbLever", true),
        customSpringLbs = if (o.isNull("springLbs")) null else o.getDouble("springLbs"),
        preloadHintResId = dev.suspension.app.R.string.hint_s_pre_generic,
        preloadRangeResId = dev.suspension.app.R.string.range_preload_generic,
    )

    /** A fresh document: one bike on its stock parts, the built-in Vorlagen, no edits. */
    fun newDocument(bikeId: String, profile: BikeProfile): JSONObject = JSONObject()
        .put("schemaVersion", SCHEMA_VERSION)
        .put("rider", JSONObject().put("weightKg", DEFAULT_WEIGHT_KG).put("tempC", DEFAULT_TEMP_C))
        .put("vorlagen", JSONArray().apply {
            BUILT_IN_VORLAGEN.forEach { put(JSONObject().put("id", it.id).put("builtIn", it.builtIn!!.tag)) }
        })
        .put("bikes", JSONArray().put(
            JSONObject()
                .put("id", bikeId)
                .put("profile", profile.id)
                .put("forkId", profile.stockForkId)
                .put("shockId", profile.stockShockId)
                .put("values", JSONObject()),
        ))
        .put("selection", JSONObject().put("bikeId", bikeId).put("vorlageId", Scenario.BASIS.tag))

    val BUILT_IN_VORLAGEN: List<Vorlage> = Scenario.ordered.map { Vorlage(it.tag, it, null) }

    private fun JSONObject.optStringOrNull(name: String): String? = if (isNull(name)) null else optString(name)
    private fun JSONObject.optIntOrNull(name: String): Int? = if (isNull(name)) null else getInt(name)
}

/** In-place changes to the garage document. Each keeps every field it doesn't touch. */
object GarageDoc {
    fun bikeJson(doc: JSONObject, bikeId: String): JSONObject {
        val bikes = doc.getJSONArray("bikes")
        return (0 until bikes.length()).map { bikes.getJSONObject(it) }.first { it.getString("id") == bikeId }
    }

    private fun rider(doc: JSONObject): JSONObject =
        doc.optJSONObject("rider") ?: JSONObject().also { doc.put("rider", it) }

    private fun selection(doc: JSONObject): JSONObject =
        doc.optJSONObject("selection") ?: JSONObject().also { doc.put("selection", it) }

    fun setWeight(doc: JSONObject, kg: Double) {
        rider(doc).put("weightKg", kg)
    }

    fun setTemp(doc: JSONObject, tempC: Int) {
        rider(doc).put("tempC", tempC)
    }

    fun selectVorlage(doc: JSONObject, vorlageId: String) {
        selection(doc).put("vorlageId", vorlageId)
    }

    fun setEdit(doc: JSONObject, bikeId: String, vorlageId: String, rowId: String, edit: Edit?) {
        val bike = bikeJson(doc, bikeId)
        val values = bike.optJSONObject("values") ?: JSONObject().also { bike.put("values", it) }
        val rows = values.optJSONObject(vorlageId) ?: JSONObject().also { values.put(vorlageId, it) }
        when (edit) {
            null -> rows.remove(rowId)
            is Edit.Delta -> rows.put(rowId, JSONObject().put("delta", edit.delta))
            is Edit.Choice -> rows.put(rowId, JSONObject().put("choice", edit.option))
        }
        if (rows.length() == 0) values.remove(vorlageId)
    }

    /** Drops the bike's edits for rows starting with [prefix] (e.g. "f_" after a fork change) in every Vorlage. */
    fun clearEdits(doc: JSONObject, bikeId: String, prefix: String = "") {
        val values = bikeJson(doc, bikeId).optJSONObject("values") ?: return
        for (vorlageId in values.keys().asSequence().toList()) {
            val rows = values.getJSONObject(vorlageId)
            rows.keys().asSequence().toList().filter { it.startsWith(prefix) }.forEach(rows::remove)
            if (rows.length() == 0) values.remove(vorlageId)
        }
    }

    fun selectFork(doc: JSONObject, bikeId: String, forkId: String, custom: ForkModel? = null) {
        val bike = bikeJson(doc, bikeId)
        bike.put("forkId", forkId)
        if (custom != null) bike.put("customFork", GarageJson.customForkJson(custom))
        clearEdits(doc, bikeId, "f_")
    }

    fun selectShock(doc: JSONObject, bikeId: String, shockId: String, custom: ShockModel? = null) {
        val bike = bikeJson(doc, bikeId)
        bike.put("shockId", shockId)
        if (custom != null) bike.put("customShock", GarageJson.customShockJson(custom))
        clearEdits(doc, bikeId, "s_")
    }

    // --- Bikes ---------------------------------------------------------------------------------

    private fun bikes(doc: JSONObject): JSONArray = doc.getJSONArray("bikes")

    fun selectBike(doc: JSONObject, bikeId: String) {
        bikeJson(doc, bikeId) // must exist
        selection(doc).put("bikeId", bikeId)
    }

    /**
     * A new bike of an unknown frame: generic profile (no flip chip, dropper or spring rule),
     * starting on [template]'s fork, shock and installed spring, no edits.
     */
    fun addBike(doc: JSONObject, newId: String, name: String, template: Bike, templateSpringLbs: Double) {
        val bike = JSONObject()
            .put("id", newId)
            .put("name", name)
            .put("profile", BikeProfiles.GENERIC_ID)
            .put("forkId", template.forkId)
            .put("shockId", template.shockId)
            .put("springLbs", templateSpringLbs)
            .put("values", JSONObject())
        template.customFork?.let { bike.put("customFork", GarageJson.customForkJson(it)) }
        template.customShock?.let { bike.put("customShock", GarageJson.customShockJson(it)) }
        bikes(doc).put(bike)
        selectBike(doc, newId)
    }

    /** An exact copy (parts, frame profile, all Vorlage values) under a new name. */
    fun copyBike(doc: JSONObject, sourceId: String, newId: String, name: String) {
        val copy = JSONObject(bikeJson(doc, sourceId).toString()).put("id", newId).put("name", name)
        bikes(doc).put(copy)
        selectBike(doc, newId)
    }

    fun renameBike(doc: JSONObject, bikeId: String, name: String) {
        bikeJson(doc, bikeId).put("name", name)
    }

    /** Removes a bike; the last one can't be deleted. Selection moves to the first remaining bike. */
    fun deleteBike(doc: JSONObject, bikeId: String) {
        val list = bikes(doc)
        require(list.length() > 1) { "Can't delete the only bike" }
        val index = (0 until list.length()).first { list.getJSONObject(it).getString("id") == bikeId }
        list.remove(index)
        if (selection(doc).optString("bikeId") == bikeId) selection(doc).put("bikeId", list.getJSONObject(0).getString("id"))
    }

    // --- Vorlagen ------------------------------------------------------------------------------

    private fun vorlagen(doc: JSONObject): JSONArray =
        doc.optJSONArray("vorlagen") ?: JSONArray().also { doc.put("vorlagen", it) }

    private fun vorlageJson(doc: JSONObject, vorlageId: String): JSONObject {
        val list = vorlagen(doc)
        return (0 until list.length()).map { list.getJSONObject(it) }.first { it.getString("id") == vorlageId }
    }

    /** A new Vorlage for every bike, starting as a copy of [copyFromId]'s values on each bike. */
    fun addVorlage(doc: JSONObject, newId: String, name: String, copyFromId: String) {
        vorlagen(doc).put(JSONObject().put("id", newId).put("name", name))
        val list = bikes(doc)
        for (i in 0 until list.length()) {
            val values = list.getJSONObject(i).optJSONObject("values") ?: continue
            values.optJSONObject(copyFromId)?.let { values.put(newId, JSONObject(it.toString())) }
        }
        selectVorlage(doc, newId)
    }

    /** Only the rider's own Vorlagen can be renamed; built-ins are translated names. */
    fun renameVorlage(doc: JSONObject, vorlageId: String, name: String) {
        val v = vorlageJson(doc, vorlageId)
        require(!v.has("builtIn")) { "Built-in Vorlagen keep their name" }
        v.put("name", name)
    }

    /** Deletes one of the rider's own Vorlagen and its values on every bike. */
    fun deleteVorlage(doc: JSONObject, vorlageId: String) {
        val list = vorlagen(doc)
        val index = (0 until list.length()).first { list.getJSONObject(it).getString("id") == vorlageId }
        require(!list.getJSONObject(index).has("builtIn")) { "Built-in Vorlagen can't be deleted" }
        list.remove(index)
        val bikeList = bikes(doc)
        for (i in 0 until bikeList.length()) bikeList.getJSONObject(i).optJSONObject("values")?.remove(vorlageId)
        if (selection(doc).optString("vorlageId") == vorlageId) selectVorlage(doc, Scenario.BASIS.tag)
    }
}

/** Legacy-compatible tag of a built-in Vorlage ("basis", "bikepark" …) — also its id. */
val Scenario.tag: String get() = name.lowercase()
