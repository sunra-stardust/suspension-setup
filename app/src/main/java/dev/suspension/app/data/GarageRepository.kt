package dev.suspension.app.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Rider weight including riding gear — Fox's charts assume "fully kitted" weight. */
const val DEFAULT_WEIGHT_KG = 98.0
const val MIN_WEIGHT_KG = 30.0
const val MAX_WEIGHT_KG = 180.0

/**
 * All of the rider's data in one JSON file (`files/garage.json`, backup-ready): rider
 * conditions, Vorlagen, bikes with their parts and relative edits.
 *
 * - **Load** is synchronous (a few KB) so the first frame already shows the rider's values.
 * - **First start after the update:** the pre-phase-3 DataStores are imported ([LegacyStorage.import]).
 * - **Every change** updates [state] at once; the file is written atomically in the background,
 *   then the first bike is mirrored into the old DataStores so a rolled-back release still
 *   shows the latest values.
 * - **Round trip:** if a rolled-back release changed the old storage (its fingerprint no longer
 *   matches the one recorded after our last mirror), those changes are imported again.
 */
class GarageRepository private constructor(context: Context) {
    private val file = File(context.filesDir, FILE_NAME)
    private val legacy = LegacyStores(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()
    private val pending = mutableListOf<Job>()

    /** Source of truth: kept as JSON so fields a newer release wrote survive our saves. */
    private var doc: JSONObject
    private val _state: MutableStateFlow<Garage>
    val state: StateFlow<Garage> get() = _state.asStateFlow()

    init {
        doc = runBlocking { loadOrMigrate() }
        _state = MutableStateFlow(GarageJson.parse(doc))
    }

    private suspend fun loadOrMigrate(): JSONObject {
        val existing = readFile()
        val old = legacy.read()
        if (existing == null) {
            val migrated = GarageJson.newDocument(newId("b"), BikeProfiles.current)
            if (!old.isEmpty) importLegacy(migrated, old)
            writeFile(migrated)
            mirror(migrated)
            return migrated
        }
        val recorded = existing.optJSONObject("legacy")?.optString("fingerprint")
        if (!old.isEmpty && recorded != old.fingerprint) {
            Log.i(TAG, "Old storage changed since the last mirror (rolled-back release?) — importing it again")
            importLegacy(existing, old)
            writeFile(existing)
            mirror(existing)
        }
        return existing
    }

    /** Replaces the mirrored bike's parts and built-in Vorlage values with what the old storage holds. */
    private fun importLegacy(target: JSONObject, old: LegacyStores.Snapshot) {
        val bikeId = mirroredBikeId(target)
        val profile = BikeParts.profile(GarageJson.parse(target).bikes.first { it.id == bikeId })
        val imported = LegacyStorage.import(old.settings, old.values, profile, bikeId)
        GarageDoc.setWeight(target, imported.weightKg)
        GarageDoc.setTemp(target, imported.tempC)
        val bikeJson = GarageDoc.bikeJson(target, bikeId)
        bikeJson.put("forkId", imported.bike.forkId).put("shockId", imported.bike.shockId)
        imported.bike.customFork?.let { bikeJson.put("customFork", GarageJson.customForkJson(it)) }
        imported.bike.customShock?.let { bikeJson.put("customShock", GarageJson.customShockJson(it)) }
        for (scenario in Scenario.ordered) {
            bikeJson.optJSONObject("values")?.remove(scenario.tag)
            imported.bike.editsFor(scenario.tag).forEach { (rowId, edit) ->
                GarageDoc.setEdit(target, bikeId, scenario.tag, rowId, edit)
            }
        }
    }

    /** Applies [change] to the document, publishes the new state and saves in the background. */
    fun update(change: (JSONObject) -> Unit) {
        synchronized(this) {
            change(doc)
            _state.value = GarageJson.parse(doc)
        }
        val job = scope.launch { persist() }
        synchronized(pending) {
            pending.removeAll { it.isCompleted }
            pending += job
        }
    }

    /** Suspends until every change made so far is on disk (and mirrored). */
    suspend fun awaitSaved() {
        synchronized(pending) { pending.toList() }.joinAll()
    }

    private suspend fun persist() = writeLock.withLock {
        val snapshot = synchronized(this) { JSONObject(doc.toString()) }
        try {
            writeFile(snapshot)
            mirror(snapshot)
            // Fingerprint and mirrored bike id belong to the live document too.
            synchronized(this) { doc.put("legacy", JSONObject(snapshot.getJSONObject("legacy").toString())) }
            writeFile(snapshot)
        } catch (e: Exception) {
            Log.e(TAG, "Saving failed", e)
        }
    }

    /** Writes the mirrored bike in the old format and records the old storage's fingerprint in [target]. */
    private suspend fun mirror(target: JSONObject) {
        val garage = GarageJson.parse(target)
        val bike = garage.bikes.first { it.id == mirroredBikeId(target) }
        val profile = BikeParts.profile(bike)
        val written = legacy.write(
            LegacyStorage.mirrorSettings(bike, garage.weightKg, garage.tempC, profile),
            LegacyStorage.mirrorValues(bike, garage.weightKg, garage.tempC, profile),
        )
        recordFingerprint(target, written.fingerprint)
    }

    private fun recordFingerprint(target: JSONObject, fingerprint: String) {
        val legacyJson = target.optJSONObject("legacy") ?: JSONObject().also { target.put("legacy", it) }
        legacyJson.put("fingerprint", fingerprint)
    }

    /** The bike the old storage mirrors: the one migrated from it, or the first bike once that one is deleted. */
    private fun mirroredBikeId(target: JSONObject): String {
        val bikes = target.getJSONArray("bikes")
        val ids = (0 until bikes.length()).map { bikes.getJSONObject(it).getString("id") }
        val legacyJson = target.optJSONObject("legacy") ?: JSONObject().also { target.put("legacy", it) }
        val recorded = legacyJson.optString("bikeId")
        if (recorded in ids) return recorded
        legacyJson.put("bikeId", ids.first())
        return ids.first()
    }

    private fun readFile(): JSONObject? = try {
        // Parsed once here so a structurally broken file is treated like an unreadable one, not a crash loop.
        if (file.exists()) JSONObject(file.readText()).also { GarageJson.parse(it) } else null
    } catch (e: Exception) {
        // A damaged file must not lock the rider out; keep it for inspection and start from the old storage.
        Log.e(TAG, "garage.json unreadable, keeping a copy and starting over", e)
        file.copyTo(File(file.parentFile, "$FILE_NAME.broken-${System.currentTimeMillis()}"), overwrite = true)
        null
    }

    /** Atomic: write a temp file, then rename over the old one. */
    private fun writeFile(content: JSONObject) {
        val tmp = File(file.parentFile, "$FILE_NAME.tmp")
        tmp.writeText(content.toString(2))
        if (!tmp.renameTo(file)) {
            file.delete()
            check(tmp.renameTo(file)) { "Could not replace $file" }
        }
    }

    // --- Rider actions -------------------------------------------------------------------------

    fun setWeight(kg: Double) = update { GarageDoc.setWeight(it, kotlin.math.round(kg).coerceIn(MIN_WEIGHT_KG, MAX_WEIGHT_KG)) }
    fun setTemp(tempC: Int) = update { GarageDoc.setTemp(it, tempC.coerceIn(MIN_TEMP_C, MAX_TEMP_C)) }
    fun selectVorlage(vorlageId: String) = update { GarageDoc.selectVorlage(it, vorlageId) }

    fun setEdit(bikeId: String, vorlageId: String, rowId: String, edit: Edit?) =
        update { GarageDoc.setEdit(it, bikeId, vorlageId, rowId, edit) }

    fun resetValues(bikeId: String) = update { GarageDoc.clearEdits(it, bikeId) }
    fun selectFork(bikeId: String, forkId: String, custom: ForkModel? = null) = update { GarageDoc.selectFork(it, bikeId, forkId, custom) }
    fun selectShock(bikeId: String, shockId: String, custom: ShockModel? = null) = update { GarageDoc.selectShock(it, bikeId, shockId, custom) }

    fun selectBike(bikeId: String) = update { GarageDoc.selectBike(it, bikeId) }
    fun renameBike(bikeId: String, name: String) = update { GarageDoc.renameBike(it, bikeId, name.trim()) }
    fun copyBike(bikeId: String, name: String) = update { GarageDoc.copyBike(it, bikeId, newId("b"), name.trim()) }
    fun deleteBike(bikeId: String) = update { GarageDoc.deleteBike(it, bikeId) }

    /** New bike on the current bike's parts; its installed spring is what the template shows in Basis. */
    fun addBike(name: String, template: Bike, templateSpringLbs: Double) =
        update { GarageDoc.addBike(it, newId("b"), name.trim(), template, templateSpringLbs) }

    fun addVorlage(name: String, copyFromId: String) = update { GarageDoc.addVorlage(it, newId("v"), name.trim(), copyFromId) }
    fun renameVorlage(vorlageId: String, name: String) = update { GarageDoc.renameVorlage(it, vorlageId, name.trim()) }
    fun deleteVorlage(vorlageId: String) = update { GarageDoc.deleteVorlage(it, vorlageId) }

    companion object {
        const val FILE_NAME = "garage.json"
        private const val TAG = "GarageRepository"

        @Volatile private var instance: GarageRepository? = null
        @Volatile private var owner: Context? = null

        /** One instance per application (tests start a fresh application per test). */
        fun get(context: Context): GarageRepository {
            val app = context.applicationContext
            synchronized(this) {
                if (instance == null || owner !== app) {
                    instance = GarageRepository(app)
                    owner = app
                }
                return instance!!
            }
        }

        /** Tests only: the next [get] reads the files again, like a fresh process start. */
        internal fun forgetInstance() {
            synchronized(this) { instance = null }
        }

        fun newId(prefix: String): String = "$prefix-${UUID.randomUUID().toString().take(8)}"
    }
}
