package dev.suspension.app.update

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ReleaseManifestTest {

    private val sha = "a".repeat(64)

    private fun json(
        tag: String = "v0.6.20",
        asset: String = "suspension-setup-0.6.20.apk",
        sha256: String = sha,
        code: Long = 20,
    ) = """{"versionCode":$code,"versionName":"0.6.20","tag":"$tag","apkAssetName":"$asset","sha256":"$sha256","notes":"- Fix"}"""

    @Test
    fun `parses the manifest the release pipeline writes`() {
        val m = ReleaseManifests.parse(json())
        assertNotNull(m)
        assertEquals(20L, m!!.versionCode)
        assertEquals("v0.6.20", m.tag)
        assertEquals("- Fix", m.notes)
    }

    @Test
    fun `rejects anything that could point the download elsewhere`() {
        assertNull(ReleaseManifests.parse(json(asset = "../evil.apk")))
        assertNull(ReleaseManifests.parse(json(asset = "https://evil.example/x.apk")))
        assertNull(ReleaseManifests.parse(json(tag = "v1/../../other")))
        assertNull(ReleaseManifests.parse(json(asset = "app.zip")))
    }

    @Test
    fun `rejects missing or malformed checksums and garbage`() {
        assertNull(ReleaseManifests.parse(json(sha256 = "123")))
        assertNull(ReleaseManifests.parse(json(sha256 = "z".repeat(64))))
        assertNull(ReleaseManifests.parse(json(code = 0)))
        assertNull(ReleaseManifests.parse("not json"))
        assertNull(ReleaseManifests.parse("""{"versionCode":5}"""))
    }

    @Test
    fun `accepts an upper-case checksum`() {
        assertEquals(sha, ReleaseManifests.parse(json(sha256 = sha.uppercase()))!!.sha256)
    }

    @Test
    fun `download url is built from the pinned repo, never from the manifest`() {
        val m = ReleaseManifests.parse(json())!!
        assertEquals(
            "https://github.com/owner/repo/releases/download/v0.6.20/suspension-setup-0.6.20.apk",
            UpdateUrls.apk("owner/repo", m),
        )
        assertEquals("https://github.com/owner/repo/releases/latest/download/latest.json", UpdateUrls.latestManifest("owner/repo"))
    }

    @Test
    fun `only a strictly higher version code is an update`() {
        val m = ReleaseManifests.parse(json(code = 20))!!
        assertTrue(isNewer(19, m))
        assertFalse(isNewer(20, m))
        assertFalse(isNewer(21, m))
    }

    @Test
    fun `automatic check runs at most every 12 hours`() {
        val h = 60 * 60 * 1000L
        assertTrue(isAutoCheckDue(lastCheckMs = 0, nowMs = 13 * h))
        assertFalse(isAutoCheckDue(lastCheckMs = 10 * h, nowMs = 13 * h))
        assertTrue(isAutoCheckDue(lastCheckMs = 20 * h, nowMs = 13 * h), "clock went backwards")
    }

    @Test
    fun `sha256 of a file`(@TempDir dir: File) {
        val file = File(dir, "x").apply { writeText("abc") }
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256Hex(file))
    }
}
