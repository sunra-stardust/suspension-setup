package dev.suspension.app.safety

import android.content.Context
import android.content.SharedPreferences

/**
 * Crash-loop detection. When a release crashes repeatedly, the next start shows a safe-mode
 * screen (update check, "start anyway") instead of the normal UI, so a broken release can
 * always be replaced by a fixed one from inside the app.
 */
object CrashLoopPolicy {
    const val THRESHOLD = 2
    const val WINDOW_MS: Long = 10 * 60 * 1000L
    /** A session that runs this long without crashing clears the history. */
    const val HEALTHY_AFTER_MS: Long = 30_000L
    private const val KEEP = 5

    fun recordCrash(history: List<Long>, nowMs: Long): List<Long> =
        (history + nowMs).filter { nowMs - it in 0..WINDOW_MS }.takeLast(KEEP)

    fun shouldEnterSafeMode(history: List<Long>, nowMs: Long): Boolean =
        history.count { nowMs - it in 0..WINDOW_MS } >= THRESHOLD
}

class CrashGuard(
    private val prefs: SharedPreferences,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    fun history(): List<Long> = prefs.getString(KEY, "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }

    fun shouldEnterSafeMode(): Boolean = CrashLoopPolicy.shouldEnterSafeMode(history(), clock())

    /** Synchronous write: the process is about to die. */
    fun recordCrash() {
        prefs.edit().putString(KEY, CrashLoopPolicy.recordCrash(history(), clock()).joinToString(",")).commit()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        private const val KEY = "crash_times"

        fun from(context: Context) = CrashGuard(context.getSharedPreferences("crash_guard", Context.MODE_PRIVATE))

        /** Records every uncaught exception, then lets the platform handle it as usual. */
        fun install(context: Context) {
            val guard = from(context)
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, error ->
                runCatching { guard.recordCrash() }
                previous?.uncaughtException(thread, error)
            }
        }
    }
}
