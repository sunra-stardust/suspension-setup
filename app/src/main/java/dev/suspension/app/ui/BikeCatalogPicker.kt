package dev.suspension.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.suspension.app.R
import dev.suspension.app.data.BikeModel
import dev.suspension.app.data.ComponentCatalog
import dev.suspension.app.ui.components.GroupCard
import dev.suspension.app.ui.components.RowDivider
import dev.suspension.app.ui.theme.AppTheme

/** Catalog bikes whose maker or name contains [query] (case-insensitive), newest model year first. */
internal fun searchBikes(bikes: List<BikeModel>, query: String): List<BikeModel> {
    val q = query.trim().lowercase()
    return bikes.filter { q.isEmpty() || it.displayName.lowercase().contains(q) }.sortedForPicker()
}

private fun List<BikeModel>.sortedForPicker(): List<BikeModel> =
    sortedWith(compareBy<BikeModel>({ it.maker.lowercase() }, { it.model.lowercase() }, { -(it.modelYears.maxOrNull() ?: 0) }, { it.trim.lowercase() }))

/**
 * "New bike": search or browse the catalog by maker → model (year, trim) → frame size.
 * Picking a bike creates it on its stock parts; "My bike isn't listed" falls back to an own bike.
 */
@Composable
fun BikeCatalogOverlay(
    onPick: (BikeModel, size: String?) -> Unit,
    onOwnBike: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    var query by rememberSaveable { mutableStateOf("") }
    var maker by rememberSaveable { mutableStateOf<String?>(null) }
    var modelId by rememberSaveable { mutableStateOf<String?>(null) }
    val model = modelId?.let(ComponentCatalog::bikeById)

    PickerScaffold(title = stringResource(R.string.bike_catalog_title), onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                model != null -> {
                    Text(text = model.displayName, style = type.groupHeading, color = colors.ink)
                    Text(text = stringResource(R.string.bike_catalog_size_hint), style = type.rowHint, color = colors.dim)
                    GroupCard {
                        model.sizes.forEachIndexed { index, size ->
                            if (index > 0) RowDivider()
                            CatalogRow(name = size, details = emptyList(), selected = false) { onPick(model, size) }
                        }
                        if (model.sizes.isNotEmpty()) RowDivider()
                        CatalogRow(name = stringResource(R.string.bike_catalog_size_other), details = emptyList(), selected = false) { onPick(model, null) }
                    }
                    ApplyButton(stringResource(R.string.bike_catalog_back)) { modelId = null }
                }

                maker == null && query.isBlank() -> {
                    SearchField(query) { query = it }
                    val makers = ComponentCatalog.bikeMakers
                    if (makers.isNotEmpty()) {
                        GroupCard {
                            makers.forEachIndexed { index, m ->
                                if (index > 0) RowDivider()
                                val count = ComponentCatalog.bikes.count { it.maker == m }
                                CatalogRow(
                                    name = m,
                                    details = listOf(pluralStringResource(R.plurals.bike_catalog_models, count, count)),
                                    selected = false,
                                ) { maker = m }
                            }
                        }
                    }
                }

                else -> {
                    if (maker == null) SearchField(query) { query = it }
                    val bikes = maker?.let { m -> ComponentCatalog.bikes.filter { it.maker == m }.sortedForPicker() }
                        ?: searchBikes(ComponentCatalog.bikes, query)
                    if (bikes.isEmpty()) {
                        Text(text = stringResource(R.string.bike_catalog_no_match), style = type.rowHint, color = colors.dim)
                    } else {
                        GroupCard {
                            bikes.forEachIndexed { index, bike ->
                                if (index > 0) RowDivider()
                                CatalogRow(
                                    name = bike.displayName,
                                    details = listOf(stringResource(R.string.bike_parts, bike.stockFork.name, bike.stockShock.name)),
                                    selected = false,
                                ) { if (bike.sizes.isEmpty()) onPick(bike, null) else modelId = bike.id }
                            }
                        }
                    }
                    if (maker != null) ApplyButton(stringResource(R.string.bike_catalog_back)) { maker = null }
                }
            }

            Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ApplyButton(stringResource(R.string.bike_catalog_not_found)) { onOwnBike() }
                Text(text = stringResource(R.string.bike_new_hint), style = type.rowHint, color = colors.dim)
            }
        }
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit) {
    GroupCard {
        CustomTextField(
            label = stringResource(R.string.bike_catalog_search_label),
            value = value,
            placeholder = stringResource(R.string.bike_catalog_search_placeholder),
            onValueChange = onChange,
        )
    }
}
