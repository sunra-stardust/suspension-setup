package dev.suspension.app.care

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.UUID

/**
 * The maintenance calendar's data in `files/care.json` (backup-ready, next to `garage.json`).
 * Same approach as the garage: loaded synchronously, every change updates [state] at once and is
 * written atomically in the background; the JSON document is the source of truth so unknown
 * fields survive.
 */
class CareRepository private constructor(context: Context) {
    private val file = File(context.filesDir, FILE_NAME)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeLock = Mutex()
    private val pending = mutableListOf<Job>()

    private val doc: JSONObject = load()
    private val _state = MutableStateFlow(CareJson.parse(doc))
    val state: StateFlow<CareData> get() = _state.asStateFlow()

    private fun load(): JSONObject = try {
        if (file.exists()) JSONObject(file.readText()).also { CareJson.parse(it) } else CareJson.newDocument()
    } catch (e: Exception) {
        // A damaged file must not lock the rider out; keep it for inspection and start empty.
        Log.e(TAG, "care.json unreadable, keeping a copy and starting over", e)
        file.copyTo(File(file.parentFile, "$FILE_NAME.broken-${System.currentTimeMillis()}"), overwrite = true)
        CareJson.newDocument()
    }

    private fun <T> update(change: (JSONObject) -> T): T {
        val result = synchronized(this) {
            val r = change(doc)
            _state.value = CareJson.parse(doc)
            r
        }
        val job = scope.launch { persist() }
        synchronized(pending) {
            pending.removeAll { it.isCompleted }
            pending += job
        }
        return result
    }

    /** Suspends until every change made so far is on disk. */
    suspend fun awaitSaved() {
        synchronized(pending) { pending.toList() }.joinAll()
    }

    private suspend fun persist() = writeLock.withLock {
        val snapshot = synchronized(this) { doc.toString(2) }
        try {
            val tmp = File(file.parentFile, "$FILE_NAME.tmp")
            tmp.writeText(snapshot)
            if (!tmp.renameTo(file)) {
                file.delete()
                check(tmp.renameTo(file)) { "Could not replace $file" }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Saving failed", e)
        }
    }

    // --- Rider actions -------------------------------------------------------------------------

    fun setPurchaseDate(date: LocalDate?) = update { CareJson.setPurchaseDate(it, date) }
    fun setOdometer(km: Int, today: LocalDate) = update { CareJson.setOdometer(it, km, today) }
    fun setReminders(enabled: Boolean) = update { CareJson.setReminders(it, enabled) }
    fun setKmPerHour(value: Int) = update { CareJson.setKmPerHour(it, value) }
    fun setTaskReminder(taskId: String, enabled: Boolean) = update { CareJson.setTaskReminder(it, taskId, enabled) }
    fun setNotified(notified: Map<String, Notified>) = update { CareJson.setNotified(it, notified) }

    /** Returns true if the entry raised the odometer. */
    fun saveEntry(entry: LogEntry, today: LocalDate): Boolean = update { CareJson.saveEntry(it, entry, today) }
    fun deleteEntry(id: String) = update { CareJson.deleteEntry(it, id) }

    companion object {
        const val FILE_NAME = "care.json"
        private const val TAG = "CareRepository"

        @Volatile private var instance: CareRepository? = null
        @Volatile private var owner: Context? = null

        /** One instance per application (tests start a fresh application per test). */
        fun get(context: Context): CareRepository {
            val app = context.applicationContext
            synchronized(this) {
                if (instance == null || owner !== app) {
                    instance = CareRepository(app)
                    owner = app
                }
                return instance!!
            }
        }

        /** Tests only: the next [get] reads the file again, like a fresh process start. */
        internal fun forgetInstance() {
            synchronized(this) { instance = null }
        }

        fun newEntryId(): String = UUID.randomUUID().toString()
    }
}
