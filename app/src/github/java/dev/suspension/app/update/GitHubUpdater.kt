package dev.suspension.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import dev.suspension.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Reads the public GitHub Releases of [repo]: `latest.json`, then the APK it names. No token needed. */
class GitHubUpdater(
    private val context: Context,
    private val repo: String,
) : Updater {

    override val installedVersionCode: Long = BuildConfig.VERSION_CODE.toLong()

    private val downloadDir: File get() = File(context.cacheDir, "updates")

    override suspend fun fetchLatest(): ReleaseManifest = withContext(Dispatchers.IO) {
        val body = open(UpdateUrls.latestManifest(repo), readTimeoutMs = 15_000).inputStream.use { it.readBytes().decodeToString() }
        ReleaseManifests.parse(body) ?: throw IOException("latest.json is malformed")
    }

    override suspend fun download(manifest: ReleaseManifest, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        clearDownloads()
        val target = File(downloadDir.apply { mkdirs() }, manifest.apkAssetName)
        val connection = open(UpdateUrls.apk(repo, manifest), readTimeoutMs = 120_000)
        val total = connection.contentLengthLong
        connection.inputStream.use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var copied = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    copied += read
                    if (total > 0) onProgress((copied.toFloat() / total).coerceIn(0f, 1f))
                }
            }
        }
        if (sha256Hex(target) != manifest.sha256) {
            target.delete()
            throw ChecksumMismatch()
        }
        target
    }

    override fun install(apk: File): InstallStart {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return InstallStart.NEEDS_PERMISSION
        }
        // Android always shows its installer screen for a sideloaded update — there is no silent path.
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return InstallStart.STARTED
    }

    override fun clearDownloads() {
        downloadDir.listFiles()?.forEach { it.delete() }
    }

    /** GitHub answers release downloads with a redirect to its CDN; HttpURLConnection follows it (https → https). */
    private fun open(url: String, readTimeoutMs: Int): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 15_000
        connection.readTimeout = readTimeoutMs
        connection.setRequestProperty("User-Agent", "suspension-setup/${BuildConfig.VERSION_NAME}")
        if (connection.responseCode !in 200..299) {
            connection.disconnect()
            throw IOException("HTTP ${connection.responseCode} for $url")
        }
        return connection
    }
}
