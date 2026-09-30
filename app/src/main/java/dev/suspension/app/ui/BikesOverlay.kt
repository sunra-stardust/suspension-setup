package dev.suspension.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.suspension.app.R
import dev.suspension.app.data.Bike
import dev.suspension.app.data.BikeModel
import dev.suspension.app.data.BikeParts
import dev.suspension.app.data.Garage
import dev.suspension.app.ui.components.ConfirmDialog
import dev.suspension.app.ui.components.GroupCard
import dev.suspension.app.ui.components.NameDialog
import dev.suspension.app.ui.components.RowDivider
import dev.suspension.app.ui.theme.AppTheme

/** Name shown for a bike: the rider's name, else the frame profile's name. */
@Composable
fun Bike.displayName(): String = name ?: stringResource(BikeParts.profile(this).nameResId)

private enum class BikeDialog { NONE, NEW, RENAME, COPY, DELETE }

/**
 * Bike list: switch, add, copy, rename, delete. Actions apply to the currently selected bike.
 * "New bike" opens the catalog ([BikeCatalogOverlay]); an own bike is the fallback from there.
 */
@Composable
fun BikesOverlay(
    garage: Garage,
    onSelect: (Bike) -> Unit,
    onAddCatalog: (BikeModel, size: String?) -> Unit,
    onAdd: (name: String) -> Unit,
    onCopy: (Bike, name: String) -> Unit,
    onRename: (Bike, name: String) -> Unit,
    onDelete: (Bike) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val current = garage.selectedBike
    val currentName = current.displayName()
    var dialog by rememberSaveable { mutableStateOf(BikeDialog.NONE) }
    var catalogOpen by rememberSaveable { mutableStateOf(false) }
    val customForkName = stringResource(R.string.custom_fork_default_name)
    val customShockName = stringResource(R.string.custom_shock_default_name)

    if (catalogOpen) {
        BikeCatalogOverlay(
            onPick = { model, size -> onAddCatalog(model, size); catalogOpen = false; onDismiss() },
            onOwnBike = { catalogOpen = false; dialog = BikeDialog.NEW },
            onDismiss = { catalogOpen = false },
        )
    } else PickerScaffold(title = stringResource(R.string.bikes_title), onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GroupCard {
                garage.bikes.forEachIndexed { index, bike ->
                    if (index > 0) RowDivider()
                    val profile = BikeParts.profile(bike)
                    val fork = BikeParts.fork(bike, profile).displayName.ifBlank { customForkName }
                    val shock = BikeParts.shock(bike, profile).displayName.ifBlank { customShockName }
                    CatalogRow(
                        name = bike.displayName(),
                        details = listOf(stringResource(R.string.bike_parts, fork, shock)),
                        selected = bike.id == current.id,
                        onClick = { onSelect(bike); onDismiss() },
                    )
                }
            }

            Text(text = currentName, style = type.groupHeading, color = colors.ink, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) { ApplyButton(stringResource(R.string.action_rename)) { dialog = BikeDialog.RENAME } }
                Column(modifier = Modifier.weight(1f)) { ApplyButton(stringResource(R.string.action_copy)) { dialog = BikeDialog.COPY } }
                if (garage.bikes.size > 1) {
                    Column(modifier = Modifier.weight(1f)) { ApplyButton(stringResource(R.string.action_delete)) { dialog = BikeDialog.DELETE } }
                }
            }

            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ApplyButton(stringResource(R.string.bike_new)) { catalogOpen = true }
            }
        }
    }

    val close = { dialog = BikeDialog.NONE }
    when (dialog) {
        BikeDialog.NEW -> NameDialog(
            title = stringResource(R.string.bike_new),
            hint = stringResource(R.string.bike_new_hint),
            initial = "",
            onConfirm = { onAdd(it); close() },
            onDismiss = close,
        )
        BikeDialog.RENAME -> NameDialog(
            title = stringResource(R.string.action_rename),
            hint = null,
            initial = currentName,
            onConfirm = { onRename(current, it); close() },
            onDismiss = close,
        )
        BikeDialog.COPY -> NameDialog(
            title = stringResource(R.string.action_copy),
            hint = null,
            initial = stringResource(R.string.bike_copy_name, currentName),
            onConfirm = { onCopy(current, it); close() },
            onDismiss = close,
        )
        BikeDialog.DELETE -> ConfirmDialog(
            text = stringResource(R.string.bike_delete_confirm, currentName),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { onDelete(current); close() },
            onDismiss = close,
        )
        BikeDialog.NONE -> Unit
    }
}
