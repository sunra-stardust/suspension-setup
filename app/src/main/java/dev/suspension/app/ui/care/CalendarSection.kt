package dev.suspension.app.ui.care

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.suspension.app.R
import dev.suspension.app.care.CareData
import dev.suspension.app.care.CareFormat
import dev.suspension.app.care.CareReminders
import dev.suspension.app.care.CareTag
import dev.suspension.app.care.DueCalculator
import dev.suspension.app.care.DueState
import dev.suspension.app.care.LogEntry
import dev.suspension.app.care.MaintCatalog
import dev.suspension.app.care.MaintGroup
import dev.suspension.app.care.TaskDue
import dev.suspension.app.ui.components.ConfirmDialog
import dev.suspension.app.ui.components.GroupCard
import dev.suspension.app.ui.components.NumberDialog
import dev.suspension.app.ui.components.RowDivider
import dev.suspension.app.ui.components.StepButton
import dev.suspension.app.ui.theme.AppTheme
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The interactive section: what is due, the bike's data, interval cards per component group, the service log. */
@Composable
fun CalendarSection(data: CareData, today: LocalDate, actions: CareActions) {
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val evaluated = remember(data, today) { DueCalculator.evaluateAll(data.dueInput(today)) }
    val dueNow = remember(evaluated) { DueCalculator.dueList(evaluated) }
    val byGroup = remember(evaluated) { evaluated.groupBy { it.task.group } }
    val historyOrder = remember(data.log) { data.log.sortedWith(compareByDescending<LogEntry> { it.date }.thenByDescending { it.createdAt }) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item(key = "due") {
            CareCardFrame(stringResource(R.string.care_due_title), null) {
                GroupCard {
                    if (dueNow.isEmpty()) {
                        Text(
                            text = stringResource(R.string.care_due_empty),
                            style = AppTheme.type.body,
                            color = AppTheme.colors.dim,
                            modifier = Modifier.padding(12.dp),
                        )
                    } else {
                        dueNow.forEachIndexed { index, due ->
                            if (index > 0) RowDivider()
                            DueSummaryRow(due)
                        }
                    }
                }
            }
        }
        item(key = "bike") { MyBikeCard(data, today, actions) }
        MaintGroup.entries.forEach { group ->
            val rows = byGroup[group].orEmpty()
            if (rows.isNotEmpty()) {
                item(key = "group-${group.name}") {
                    CareCardFrame(stringResource(group.titleRes), null) {
                        group.noteRes?.let { note ->
                            Column {
                                Text(text = stringResource(note), style = AppTheme.type.rowHint, color = AppTheme.colors.dim)
                                group.noteTag?.let { TagLine(it) }
                            }
                        }
                        GroupCard {
                            rows.forEachIndexed { index, due ->
                                if (index > 0) RowDivider()
                                TaskRow(due, data, actions)
                            }
                        }
                    }
                }
            }
        }
        item(key = "history") {
            CareCardFrame(stringResource(R.string.care_history_title), null) {
                GroupCard {
                    if (historyOrder.isEmpty()) {
                        Text(
                            text = stringResource(R.string.care_history_empty),
                            style = AppTheme.type.body,
                            color = AppTheme.colors.dim,
                            modifier = Modifier.padding(12.dp),
                        )
                    } else {
                        historyOrder.forEachIndexed { index, entry ->
                            if (index > 0) RowDivider()
                            HistoryRow(entry) { actions.onEditEntry(entry.id) }
                        }
                    }
                }
            }
        }
        item(key = "footer") { SectionFooter() }
    }
}

/** Compact row of the `Fällig` card: name, status and what is left. */
@Composable
private fun DueSummaryRow(due: TaskDue) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val remaining = remainingText(DueCalculator.remainingParts(due))
    val status = statusLabel(due.state)
    val tag = stringResource(due.task.tag.labelResId)
    val name = stringResource(due.task.nameRes)
    val description = if (remaining.isEmpty()) stringResource(R.string.care_a11y_task_plain, name, status, tag)
    else stringResource(R.string.care_a11y_task, name, status, remaining, tag)
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text = name, style = type.rowLabel, color = colors.ink)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StatusPill(due.state)
            if (remaining.isNotEmpty()) Text(text = remaining, style = type.rowHint, color = colors.dim)
        }
    }
}

/** Collapsible task row (interval cards). */
@Composable
private fun TaskRow(due: TaskDue, data: CareData, actions: CareActions) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val task = due.task
    var expanded by rememberSaveable(task.id) { mutableStateOf(false) }

    val name = stringResource(task.nameRes)
    val status = statusLabel(due.state)
    val remaining = remainingText(DueCalculator.remainingParts(due))
    val tag = stringResource(task.tag.labelResId)
    val description = if (remaining.isEmpty()) stringResource(R.string.care_a11y_task_plain, name, status, tag)
    else stringResource(R.string.care_a11y_task, name, status, remaining, tag)
    val expandedText = stringResource(if (expanded) R.string.care_expanded else R.string.care_collapsed)

    Column {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = name) { expanded = !expanded }
                .semantics(mergeDescendants = true) {
                    contentDescription = description
                    stateDescription = expandedText
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(text = name, style = type.rowLabel, color = colors.ink)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusPill(due.state)
                if (remaining.isNotEmpty()) Text(text = remaining, style = type.rowHint, color = colors.dim)
            }
            dueLine(due)?.let { Text(text = it, style = type.rowHint, color = colors.dim) }
            Text(text = lastLine(due), style = type.rowHint, color = colors.dim)
        }
        if (expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            ) {
                Column {
                    Text(text = stringResource(task.intervalRes), style = type.body, color = colors.ink)
                    TagLine(task.tag, Modifier.padding(top = 2.dp))
                }
                Text(text = stringResource(task.noteRes), style = type.body, color = colors.ink)
                CareSwitchRow(
                    label = stringResource(R.string.care_task_reminder),
                    checked = data.remindFor(task),
                    onCheckedChange = { actions.onTaskReminder(task.id, it) },
                    horizontalPadding = 0.dp,
                )
                CareButton(
                    label = stringResource(R.string.care_task_done),
                    onClick = { actions.onLogService(listOf(task.id)) },
                )
            }
        }
    }
}

/** `Fällig bei 1.240 km oder am 12.03.2027` (only the parts that exist); null if there is no due point. */
@Composable
private fun dueLine(due: TaskDue): String? {
    if (due.state == DueState.DONE) return null
    val km = due.dueKm?.let { CareFormat.km(it) }
    val date = due.dueDate?.let { CareFormat.date(it) }
    return when {
        km != null && date != null -> stringResource(R.string.care_due_at_km_or_date, km, date)
        km != null -> stringResource(R.string.care_due_at_km, km)
        date != null -> stringResource(R.string.care_due_at_date, date)
        else -> null
    }
}

@Composable
private fun lastLine(due: TaskDue): String {
    val date = due.lastDate
    val km = due.lastKm
    return if (date != null && km != null) stringResource(R.string.care_last_done, CareFormat.date(date), CareFormat.km(km))
    else stringResource(R.string.care_last_never)
}

@Composable
private fun HistoryRow(entry: LogEntry, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val unknown = stringResource(R.string.care_unknown_task)
    val names = entry.taskIds.map { id -> MaintCatalog.byId[id]?.let { stringResource(it.nameRes) } ?: unknown }.joinToString(" · ")
    val head = stringResource(R.string.care_history_head, CareFormat.date(entry.date), CareFormat.km(entry.km))
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = head, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(text = head, style = type.rowLabel, color = colors.ink)
        if (names.isNotEmpty()) Text(text = names, style = type.body, color = colors.ink)
        if (entry.note.isNotBlank()) Text(text = entry.note, style = type.body, color = colors.dim)
    }
}

/** `Mein Rad`: purchase date, odometer, reminders, riding speed, and the entry button. */
@Composable
private fun MyBikeCard(data: CareData, today: LocalDate, actions: CareActions) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val context = LocalContext.current

    var showOdometer by rememberSaveable { mutableStateOf(false) }
    var lowerKm by rememberSaveable { mutableStateOf<Int?>(null) }

    // Notification permission: asked only when the rider switches reminders on; no repeated prompting.
    var denied by rememberSaveable { mutableStateOf(false) }
    var permissionTick by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val lifecycle = context.findActivity()?.lifecycle
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) permissionTick++ }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    val permissionGranted = remember(permissionTick, data.remindersEnabled, denied) { CareReminders.hasPermission(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        actions.onReminders(granted)
    }
    val showDenied = CareReminders.needsPermission() && !permissionGranted && (denied || data.remindersEnabled)

    CareCardFrame(stringResource(R.string.care_bike_title), null) {
        GroupCard {
            ValueRow(
                label = stringResource(R.string.care_purchase_date),
                value = data.purchaseDate?.let { CareFormat.date(it) } ?: stringResource(R.string.care_not_set),
                subLine = null,
                onClick = {
                    showDatePicker(
                        context = context,
                        initial = data.purchaseDate ?: today,
                        max = today,
                        clearLabel = context.getString(R.string.care_date_clear),
                        onClear = if (data.purchaseDate != null) ({ actions.onPurchaseDate(null) }) else null,
                        onPicked = { actions.onPurchaseDate(it) },
                    )
                },
            )
            RowDivider()
            val asOf = data.odometerUpdatedOn?.let { updated ->
                val old = ChronoUnit.DAYS.between(updated, today) > CareData.STALE_ODOMETER_DAYS
                stringResource(if (old) R.string.care_odometer_asof_old else R.string.care_odometer_asof, CareFormat.date(updated))
            }
            ValueRow(
                label = stringResource(R.string.care_odometer),
                value = stringResource(R.string.unit_km_value, CareFormat.km(data.odometerKm)),
                subLine = asOf,
                onClick = { showOdometer = true },
            )
            RowDivider()
            CareSwitchRow(
                label = stringResource(R.string.care_reminders),
                checked = data.remindersEnabled,
                onCheckedChange = { on ->
                    when {
                        !on -> { denied = false; actions.onReminders(false) }
                        permissionGranted -> { denied = false; actions.onReminders(true) }
                        else -> launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
            if (showDenied) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                ) {
                    Text(text = stringResource(R.string.care_notif_denied), style = type.body, color = colors.ink)
                    CareButton(
                        label = stringResource(R.string.care_open_settings),
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        },
                    )
                }
            }
            RowDivider()
            KmPerHourRow(value = data.kmPerHour, onChange = actions.onKmPerHour)
            RowDivider()
            Box(modifier = Modifier.padding(12.dp)) {
                CareButton(label = stringResource(R.string.care_log_service), primary = true, onClick = { actions.onLogService(emptyList()) })
            }
        }
    }

    if (showOdometer) {
        NumberDialog(
            title = stringResource(R.string.care_odo_dialog_title),
            label = stringResource(R.string.care_odo_field),
            hint = stringResource(R.string.care_odo_hint),
            initial = data.odometerKm.toString(),
            onConfirm = { km ->
                showOdometer = false
                if (km < data.odometerKm) lowerKm = km else actions.onOdometer(km)
            },
            onDismiss = { showOdometer = false },
        )
    }
    lowerKm?.let { km ->
        ConfirmDialog(
            text = stringResource(R.string.care_odo_confirm_lower, CareFormat.km(km), CareFormat.km(data.odometerKm)),
            confirmLabel = stringResource(R.string.care_apply),
            onConfirm = { lowerKm = null; actions.onOdometer(km) },
            onDismiss = { lowerKm = null },
        )
    }
}

/** Tappable row: label (and optional sub-line) left, value right. */
@Composable
private fun ValueRow(label: String, value: String, subLine: String?, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClickLabel = label, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = type.rowLabel, color = colors.ink)
            if (subLine != null) Text(text = subLine, style = type.rowHint, color = colors.dim)
        }
        Text(text = value, style = type.rowLabel, color = colors.ink)
    }
}

/** `Fahrzeit in km`: how many km one riding hour is worth when converting hour-based intervals. */
@Composable
private fun KmPerHourRow(value: Int, onChange: (Int) -> Unit) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val label = stringResource(R.string.care_km_per_hour)
    // Label above, controls below: nothing clips at 320dp and 200 % font scale.
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column {
            Text(text = label, style = type.rowLabel, color = colors.ink)
            TagLine(CareTag.SCHAETZUNG)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton(
                symbol = "−",
                contentDescription = stringResource(R.string.stepper_decrease, label),
                onClick = { if (value > CareData.MIN_KM_PER_HOUR) onChange(value - 1) },
            )
            Text(
                text = stringResource(R.string.care_km_per_hour_value, value),
                style = type.rowLabel,
                color = colors.ink,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                textAlign = TextAlign.Center,
            )
            StepButton(
                symbol = "+",
                contentDescription = stringResource(R.string.stepper_increase, label),
                onClick = { if (value < CareData.MAX_KM_PER_HOUR) onChange(value + 1) },
            )
        }
    }
}
