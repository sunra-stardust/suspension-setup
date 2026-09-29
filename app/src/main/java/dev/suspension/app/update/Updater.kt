package dev.suspension.app.update

import java.io.File

/**
 * What the UI needs from the distribution channel. Only the GitHub build has one
 * (src/github/…/Distribution.kt); on Google Play, [Distribution.updater] returns null.
 */
interface Updater {
    val installedVersionCode: Long

    /** Throws on network or format errors. */
    suspend fun fetchLatest(): ReleaseManifest

    /** Downloads into the cache and verifies the checksum; throws [ChecksumMismatch] if it doesn't match. */
    suspend fun download(manifest: ReleaseManifest, onProgress: (Float) -> Unit): File

    /** Hands [apk] to the system installer, or first opens the one-time "install unknown apps" setting. */
    fun install(apk: File): InstallStart

    /** Removes leftover APKs from earlier downloads. */
    fun clearDownloads()
}

enum class InstallStart { STARTED, NEEDS_PERMISSION }

class ChecksumMismatch : Exception("Downloaded APK doesn't match the published checksum")
