package dev.suspension.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.suspension.app.BuildConfig
import dev.suspension.app.R
import dev.suspension.app.update.FailureReason
import dev.suspension.app.update.UpdateState
import dev.suspension.app.update.UpdateViewModel
import dev.suspension.app.ui.components.GroupCard
import dev.suspension.app.ui.theme.AppTheme
import kotlin.math.roundToInt

/** Top-of-app notice while an update is available, downloading or waiting for the installer. */
@Composable
fun UpdateBanner(updates: UpdateViewModel) {
    val state by updates.state.collectAsState()
    val dismissed by updates.bannerDismissed.collectAsState()
    val visible = when (state) {
        is UpdateState.Available, is UpdateState.Downloading, is UpdateState.ReadyToInstall, is UpdateState.Failed -> true
        else -> false
    }
    if (!visible || dismissed) return

    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        GroupCard {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UpdateStatus(state, showNotes = true)
                UpdateActions(state, onInstall = updates::install, onLater = updates::dismissBanner, onCheck = { updates.check() })
            }
        }
    }
}

/** "App" card on Basics: installed version and the manual update check. */
@Composable
fun AppUpdateSection(updates: UpdateViewModel?) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    Text(stringResource(R.string.app_version, BuildConfig.VERSION_NAME), style = type.body, color = colors.ink)
    if (updates == null) {
        Text(stringResource(R.string.update_via_store), style = type.rowHint, color = colors.dim)
        return
    }
    val state by updates.state.collectAsState()
    UpdateStatus(state, showNotes = false)
    UpdateActions(state, onInstall = updates::install, onLater = null, onCheck = { updates.check() })
}

/**
 * Shown instead of the normal UI after repeated crashes. Offers the fix (an update) and a way
 * to try the normal UI again; stored settings are never touched here.
 */
@Composable
fun SafeModeScreen(updates: UpdateViewModel?, storeUrl: String?, onContinue: () -> Unit) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.safe_mode_title), style = type.screenTitle, color = colors.ink)
        Text(stringResource(R.string.safe_mode_body), style = type.body, color = colors.ink)
        Text(stringResource(R.string.app_version, BuildConfig.VERSION_NAME), style = type.rowHint, color = colors.dim)
        if (updates != null) {
            val state by updates.state.collectAsState()
            UpdateStatus(state, showNotes = true)
            UpdateActions(state, onInstall = updates::install, onLater = null, onCheck = { updates.check() })
        }
        if (storeUrl != null) {
            ActionButton(stringResource(R.string.safe_mode_store), primary = true) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        ActionButton(stringResource(R.string.safe_mode_continue), primary = false, onClick = onContinue)
    }
}

@Composable
private fun UpdateStatus(state: UpdateState, showNotes: Boolean) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val text = when (state) {
        UpdateState.Idle -> null
        UpdateState.Checking -> stringResource(R.string.update_checking)
        UpdateState.UpToDate -> stringResource(R.string.update_up_to_date)
        is UpdateState.Available -> stringResource(R.string.update_available, state.manifest.versionName)
        is UpdateState.Downloading ->
            stringResource(R.string.update_downloading, state.manifest.versionName, (state.progress * 100).roundToInt())
        is UpdateState.ReadyToInstall ->
            if (state.needsPermission) stringResource(R.string.update_needs_permission)
            else stringResource(R.string.update_ready, state.manifest.versionName)
        is UpdateState.Failed -> when (state.reason) {
            FailureReason.OFFLINE -> stringResource(R.string.update_failed_offline)
            FailureReason.CHECKSUM -> stringResource(R.string.update_failed_checksum)
        }
    } ?: return
    Text(text, style = type.rowLabel, color = colors.ink)
    val notes = (state as? UpdateState.Available)?.manifest?.notes.orEmpty()
    if (showNotes && notes.isNotBlank()) {
        Text(notes.lines().filter { it.isNotBlank() }.take(4).joinToString("\n"), style = type.rowHint, color = colors.dim)
    }
}

@Composable
private fun UpdateActions(state: UpdateState, onInstall: () -> Unit, onLater: (() -> Unit)?, onCheck: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        when (state) {
            is UpdateState.Available, is UpdateState.ReadyToInstall ->
                ActionButton(stringResource(R.string.update_install), primary = true, modifier = Modifier.weight(1f), onClick = onInstall)
            is UpdateState.Downloading, UpdateState.Checking -> Unit
            else -> ActionButton(stringResource(R.string.update_check), primary = onLater == null, modifier = Modifier.weight(1f), onClick = onCheck)
        }
        if (onLater != null && state !is UpdateState.Downloading) {
            ActionButton(stringResource(R.string.update_later), primary = false, modifier = Modifier.weight(1f), onClick = onLater)
        }
    }
}

@Composable
private fun ActionButton(label: String, primary: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val shape = RoundedCornerShape(10.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (primary) colors.hit else Color.Transparent)
            .border(1.dp, if (primary) Color.Transparent else colors.line, shape)
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 12.dp),
    ) {
        Text(text = label, style = AppTheme.type.rowLabel, color = colors.ink)
    }
}
