package dev.suspension.app.update

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateViewModelTest {

    private val manifest = ReleaseManifest(20, "0.6.20", "v0.6.20", "app.apk", "a".repeat(64), "- Fix")

    private class FakeUpdater(
        var latest: Result<ReleaseManifest>,
        var downloadResult: Result<File> = Result.success(File("app.apk")),
        var installResult: InstallStart = InstallStart.STARTED,
    ) : Updater {
        override val installedVersionCode = 19L
        var installs = 0
        override suspend fun fetchLatest() = latest.getOrThrow()
        override suspend fun download(manifest: ReleaseManifest, onProgress: (Float) -> Unit): File {
            onProgress(0.5f)
            return downloadResult.getOrThrow()
        }
        override fun install(apk: File): InstallStart {
            installs++
            return installResult
        }
        override fun clearDownloads() = Unit
    }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        UpdatePolicy.autoCheckEnabled = true
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `newer release becomes available`() {
        val vm = UpdateViewModel(FakeUpdater(Result.success(manifest)), FakePrefs())
        vm.check()
        assertEquals(UpdateState.Available(manifest), vm.state.value)
    }

    @Test
    fun `manual check reports up to date and offline, the silent one stays quiet`() {
        val updater = FakeUpdater(Result.success(manifest.copy(versionCode = 19)))
        val vm = UpdateViewModel(updater, FakePrefs())
        vm.check()
        assertEquals(UpdateState.UpToDate, vm.state.value)

        updater.latest = Result.failure(IOException("offline"))
        vm.check()
        assertEquals(UpdateState.Failed(FailureReason.OFFLINE), vm.state.value)

        val quiet = UpdateViewModel(updater, FakePrefs())
        quiet.autoCheck()
        assertEquals(UpdateState.Idle, quiet.state.value)
    }

    @Test
    fun `auto check respects the interval`() {
        val prefs = FakePrefs()
        var now = 50_000_000_000L
        val updater = FakeUpdater(Result.success(manifest))
        val vm = UpdateViewModel(updater, prefs, clock = { now })
        vm.autoCheck()
        assertEquals(UpdateState.Available(manifest), vm.state.value)

        updater.latest = Result.failure(IllegalStateException("must not be called"))
        now += 60_000
        vm.autoCheck()
        assertEquals(UpdateState.Available(manifest), vm.state.value, "second check within 12 h is skipped")
    }

    @Test
    fun `install downloads, verifies and hands over to the installer`() {
        val updater = FakeUpdater(Result.success(manifest))
        val vm = UpdateViewModel(updater, FakePrefs())
        vm.check()
        vm.install()
        val state = vm.state.value
        assertTrue(state is UpdateState.ReadyToInstall && !state.needsPermission)
        assertEquals(1, updater.installs)
    }

    @Test
    fun `corrupted download is never installed`() {
        val updater = FakeUpdater(Result.success(manifest), downloadResult = Result.failure(ChecksumMismatch()))
        val vm = UpdateViewModel(updater, FakePrefs())
        vm.check()
        vm.install()
        assertEquals(UpdateState.Failed(FailureReason.CHECKSUM), vm.state.value)
        assertEquals(0, updater.installs)
    }

    @Test
    fun `missing install permission keeps the download for a second tap`() {
        val updater = FakeUpdater(Result.success(manifest), installResult = InstallStart.NEEDS_PERMISSION)
        val vm = UpdateViewModel(updater, FakePrefs())
        vm.check()
        vm.install()
        assertTrue((vm.state.value as UpdateState.ReadyToInstall).needsPermission)

        updater.installResult = InstallStart.STARTED
        vm.install()
        assertTrue(!(vm.state.value as UpdateState.ReadyToInstall).needsPermission)
        assertEquals(2, updater.installs)
    }
}

/** Minimal in-memory SharedPreferences for plain JVM tests. */
private class FakePrefs : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()
    override fun getAll(): MutableMap<String, *> = values
    override fun getString(key: String, defValue: String?) = values[key] as String? ?: defValue
    override fun getStringSet(key: String, defValues: MutableSet<String>?) = defValues
    override fun getInt(key: String, defValue: Int) = values[key] as Int? ?: defValue
    override fun getLong(key: String, defValue: Long) = values[key] as Long? ?: defValue
    override fun getFloat(key: String, defValue: Float) = values[key] as Float? ?: defValue
    override fun getBoolean(key: String, defValue: Boolean) = values[key] as Boolean? ?: defValue
    override fun contains(key: String) = key in values
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        override fun putString(key: String, value: String?) = apply { values[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?) = this
        override fun putInt(key: String, value: Int) = apply { values[key] = value }
        override fun putLong(key: String, value: Long) = apply { values[key] = value }
        override fun putFloat(key: String, value: Float) = apply { values[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { values[key] = value }
        override fun remove(key: String) = apply { values.remove(key) }
        override fun clear() = apply { values.clear() }
        override fun commit() = true
        override fun apply() = Unit
    }
}
