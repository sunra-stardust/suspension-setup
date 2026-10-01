package dev.suspension.app.ui.care

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.suspension.app.R
import dev.suspension.app.care.CareFormat
import dev.suspension.app.care.CareRepository
import dev.suspension.app.care.EntryValidation
import dev.suspension.app.care.LogEntry
import dev.suspension.app.care.MaintCatalog
import dev.suspension.app.care.MaintGroup
import dev.suspension.app.ui.components.AppTextField
import dev.suspension.app.ui.components.ConfirmDialog
import dev.suspension.app.ui.theme.AppTheme
import java.time.LocalDate

/**
 * Full-screen entry for a completed service (new, or [initial] when editing). Nothing is written
 * until Save; the form survives rotation and process death.
 */
@Composable
fun ServiceEntryScreen(
    initial: LogEntry?,
    preselected: List<String>,
    odometerKm: Int,
    today: LocalDate,
    onSave: (LogEntry) -> Unit,
    onDelete: (() -> Unit)?,
    onCancel: () -> Unit,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val context = LocalContext.current

    var dateEpoch by rememberSaveable { mutableLongStateOf((initial?.date ?: today).toEpochDay()) }
    var kmText by rememberSaveable { mutableStateOf((initial?.km ?: odometerKm).toString()) }
    var selectedCsv by rememberSaveable { mutableStateOf((initial?.taskIds ?: preselected).joinToString(",")) }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var attempted by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val date = LocalDate.ofEpochDay(dateEpoch)
    val selected = selectedCsv.split(",").filter { it.isNotEmpty() }.toSet()
    val errors = EntryValidation.validate(date, kmText, selected, note, today)
    val km = EntryValidation.parseKm(kmText)

    BackHandler(onBack = onCancel)
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            // Swallow taps so nothing underneath the full-screen entry reacts.
            .pointerInput(Unit) { detectTapGestures { } }
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(if (initial == null) R.string.care_log_service else R.string.care_entry_edit_title),
            style = type.screenTitle,
            color = colors.ink,
            modifier = Modifier.semantics { heading() },
        )

        // Date
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val dateLabel = stringResource(R.string.care_field_date)
            Text(text = dateLabel, style = type.rowHint, color = colors.dim)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.hit)
                    .clickable(onClickLabel = dateLabel) {
                        showDatePicker(
                            context = context,
                            initial = minOf(date, today),
                            max = today,
                            clearLabel = "",
                            onClear = null,
                            onPicked = { dateEpoch = it.toEpochDay() },
                        )
                    }
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) { Text(text = CareFormat.date(date), style = type.body, color = colors.ink) }
            if (attempted && errors.dateInFuture) ErrorText(stringResource(R.string.care_err_future))
        }

        // Odometer
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppTextField(
                value = kmText,
                onValueChange = { kmText = it.filter(Char::isDigit).take(EntryValidation.MAX_KM_DIGITS) },
                label = stringResource(R.string.care_odometer),
                keyboardType = KeyboardType.Number,
            )
            if (attempted && errors.kmMissing) ErrorText(stringResource(R.string.care_err_km_missing))
            if (km != null && km < odometerKm) {
                Text(
                    text = stringResource(R.string.care_entry_below_odometer, CareFormat.km(odometerKm)),
                    style = type.rowHint,
                    color = colors.dim,
                )
            }
        }

        // Tasks
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.care_field_tasks),
                style = type.groupHeading,
                color = colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            fun toggle(id: String, on: Boolean) {
                selectedCsv = (if (on) selected + id else selected - id).joinToString(",")
            }
            MaintGroup.entries.forEach { group ->
                val tasks = MaintCatalog.tasks.filter { it.group == group }
                if (tasks.isNotEmpty()) {
                    Text(
                        text = stringResource(group.titleRes),
                        style = type.rowHint,
                        color = colors.dim,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    tasks.forEach { task ->
                        CheckRow(stringResource(task.nameRes), task.id in selected) { toggle(task.id, it) }
                    }
                }
            }
            // Ids an older entry carried that the catalog no longer knows stay in the entry.
            selected.filter { it !in MaintCatalog.byId }.forEach { id ->
                CheckRow(stringResource(R.string.care_unknown_task), true) { toggle(id, it) }
            }
        }

        // What was done
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppTextField(
                value = note,
                onValueChange = { note = it.take(EntryValidation.MAX_NOTE_LENGTH) },
                label = stringResource(R.string.care_field_note),
                hint = stringResource(R.string.care_note_hint),
                singleLine = false,
                minLines = 3,
            )
            if (attempted && errors.nothingDone) ErrorText(stringResource(R.string.care_err_nothing))
        }

        CareButton(
            label = stringResource(R.string.action_save),
            primary = true,
            onClick = {
                attempted = true
                if (!errors.any && km != null) {
                    val ordered = MaintCatalog.tasks.map { it.id }.filter { it in selected } + selected.filter { it !in MaintCatalog.byId }
                    onSave(
                        LogEntry(
                            id = initial?.id ?: CareRepository.newEntryId(),
                            date = date,
                            km = km,
                            taskIds = ordered,
                            note = note,
                            createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                        ),
                    )
                }
            },
        )
        CareButton(label = stringResource(R.string.action_cancel), onClick = onCancel)
        if (onDelete != null) {
            CareButton(label = stringResource(R.string.action_delete), onClick = { confirmDelete = true })
        }
    }

    if (confirmDelete && onDelete != null) {
        ConfirmDialog(
            text = stringResource(R.string.care_entry_delete_confirm),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Validation message: words in ink, no warning colours on this tab. */
@Composable
private fun ErrorText(text: String) {
    Text(text = text, style = AppTheme.type.rowLabel, color = AppTheme.colors.ink)
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = AppTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .padding(vertical = 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) colors.ink else colors.surface)
                .border(1.5.dp, colors.ink, RoundedCornerShape(6.dp)),
        ) {
            if (checked) Text(text = "✓", style = AppTheme.type.rowLabel, color = colors.bg)
        }
        Text(text = label, style = AppTheme.type.body, color = colors.ink, modifier = Modifier.weight(1f))
    }
}
