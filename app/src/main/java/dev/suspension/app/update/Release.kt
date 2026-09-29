package dev.suspension.app.update

import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * The `latest.json` asset every GitHub Release carries (written by .github/workflows/pipeline.yml).
 * The APK URL is never taken from the file: it's rebuilt from the pinned repo + [tag] + [apkAssetName],
 * and the download must match [sha256] before it reaches the installer.
 */
data class ReleaseManifest(
    val versionCode: Long,
    val versionName: String,
    val tag: String,
    val apkAssetName: String,
    val sha256: String,
    val notes: String,
)

object ReleaseManifests {
    private val SAFE_NAME = Regex("[A-Za-z0-9._-]+")
    private val SHA256_HEX = Regex("[0-9a-f]{64}")

    /** Null when the JSON is malformed or any field fails validation. */
    fun parse(json: String): ReleaseManifest? = runCatching {
        val o = JSONObject(json)
        ReleaseManifest(
            versionCode = o.getLong("versionCode"),
            versionName = o.getString("versionName"),
            tag = o.getString("tag"),
            apkAssetName = o.getString("apkAssetName"),
            sha256 = o.getString("sha256").lowercase(),
            notes = o.optString("notes"),
        )
    }.getOrNull()?.takeIf { it.isValid() }

    private fun ReleaseManifest.isValid(): Boolean =
        versionCode > 0 &&
            versionName.isNotBlank() &&
            SAFE_NAME.matches(tag) &&
            SAFE_NAME.matches(apkAssetName) &&
            apkAssetName.endsWith(".apk") &&
            SHA256_HEX.matches(sha256)
}

object UpdateUrls {
    fun latestManifest(repo: String) = "https://github.com/$repo/releases/latest/download/latest.json"
    fun apk(repo: String, manifest: ReleaseManifest) =
        "https://github.com/$repo/releases/download/${manifest.tag}/${manifest.apkAssetName}"
}

/** An update is offered only for a strictly higher version code — Android refuses downgrades anyway. */
fun isNewer(installedVersionCode: Long, manifest: ReleaseManifest): Boolean = manifest.versionCode > installedVersionCode

/** Background check at most every 12 h; a clock set backwards counts as "due". */
const val AUTO_CHECK_INTERVAL_MS: Long = 12 * 60 * 60 * 1000L

fun isAutoCheckDue(lastCheckMs: Long, nowMs: Long): Boolean =
    nowMs < lastCheckMs || nowMs - lastCheckMs >= AUTO_CHECK_INTERVAL_MS

fun sha256Hex(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
