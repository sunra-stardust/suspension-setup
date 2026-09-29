package dev.suspension.app.update

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val manifest: ReleaseManifest) : UpdateState
    data class Downloading(val manifest: ReleaseManifest, val progress: Float) : UpdateState
    /** Downloaded and verified; waiting for the installer or for the install permission. */
    data class ReadyToInstall(val manifest: ReleaseManifest, val apk: File, val needsPermission: Boolean) : UpdateState
    data class Failed(val reason: FailureReason) : UpdateState
}

enum class FailureReason { OFFLINE, CHECKSUM }

object UpdatePolicy {
    /** Tests switch the automatic check off so they never touch the network. */
    @Volatile
    var autoCheckEnabled: Boolean = true
}

class UpdateViewModel(
    private val updater: Updater,
    private val prefs: SharedPreferences,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /** The banner can be hidden until the next app start. */
    private val _bannerDismissed = MutableStateFlow(false)
    val bannerDismissed: StateFlow<Boolean> = _bannerDismissed.asStateFlow()

    init {
        updater.clearDownloads()
    }

    /** Quiet background check: only an available update becomes visible, errors stay silent. */
    fun autoCheck() {
        if (!UpdatePolicy.autoCheckEnabled) return
        if (!isAutoCheckDue(prefs.getLong(KEY_LAST_CHECK, 0L), clock())) return
        check(silent = true)
    }

    fun check(silent: Boolean = false) {
        if (_state.value is UpdateState.Checking || _state.value is UpdateState.Downloading) return
        if (!silent) _state.value = UpdateState.Checking
        viewModelScope.launch {
            val result = runCatching { updater.fetchLatest() }
            prefs.edit().putLong(KEY_LAST_CHECK, clock()).apply()
            val manifest = result.getOrNull()
            _state.value = when {
                manifest != null && isNewer(updater.installedVersionCode, manifest) -> UpdateState.Available(manifest)
                manifest != null -> if (silent) UpdateState.Idle else UpdateState.UpToDate
                silent -> UpdateState.Idle
                else -> UpdateState.Failed(FailureReason.OFFLINE)
            }
        }
    }

    /** Downloads (if needed) and starts the installer. */
    fun install() {
        when (val current = _state.value) {
            is UpdateState.Available -> download(current.manifest)
            is UpdateState.ReadyToInstall -> startInstaller(current.manifest, current.apk)
            else -> Unit
        }
    }

    fun dismissBanner() {
        _bannerDismissed.value = true
    }

    private fun download(manifest: ReleaseManifest) {
        _state.value = UpdateState.Downloading(manifest, 0f)
        viewModelScope.launch {
            val result = runCatching {
                updater.download(manifest) { progress -> _state.value = UpdateState.Downloading(manifest, progress) }
            }
            result.fold(
                onSuccess = { apk -> startInstaller(manifest, apk) },
                onFailure = { error ->
                    _state.value = UpdateState.Failed(if (error is ChecksumMismatch) FailureReason.CHECKSUM else FailureReason.OFFLINE)
                },
            )
        }
    }

    private fun startInstaller(manifest: ReleaseManifest, apk: File) {
        val started = updater.install(apk)
        _state.value = UpdateState.ReadyToInstall(manifest, apk, needsPermission = started == InstallStart.NEEDS_PERMISSION)
    }

    private companion object {
        const val KEY_LAST_CHECK = "last_check_ms"
    }
}
