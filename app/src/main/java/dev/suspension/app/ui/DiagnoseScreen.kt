package dev.suspension.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.suspension.app.R
import dev.suspension.app.data.DiagnoseData
import dev.suspension.app.data.DiagnoseEntry
import dev.suspension.app.ui.theme.AppTheme

@Composable
fun DiagnoseScreen(listState: LazyListState) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    var query by rememberSaveable { mutableStateOf("") }
    var expandedIds by rememberSaveable(
        stateSaver = listSaver<MutableSet<Int>, Int>(
            save = { it.toList() },
            restore = { it.toMutableSet() },
        ),
    ) { mutableStateOf(mutableSetOf()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.hit)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.diagnose_search_placeholder),
                    style = type.body,
                    color = colors.dim,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                textStyle = TextStyle(color = colors.ink, fontSize = type.body.fontSize, fontFamily = type.body.fontFamily),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.ink),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // stringResource must be called unconditionally over the fixed, statically-ordered
        // entry list — safe here since every recomposition iterates the same 19 entries.
        val resolved = DiagnoseData.entries.map { entry ->
            ResolvedEntry(entry, stringResource(entry.symptomResId), stringResource(entry.actionResId), stringResource(entry.explanationResId))
        }
        val q = query.trim().lowercase()
        val filtered = remember(q, resolved) {
            if (q.isEmpty()) {
                resolved
            } else {
                resolved.filter { it.symptom.lowercase().contains(q) || it.action.lowercase().contains(q) }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            items(filtered, key = { it.entry.symptomResId }) { resolvedEntry ->
                DiagnoseCard(
                    resolved = resolvedEntry,
                    expanded = resolvedEntry.entry.symptomResId in expandedIds,
                    onToggle = {
                        expandedIds = expandedIds.toMutableSet().apply {
                            val id = resolvedEntry.entry.symptomResId
                            if (id in this) remove(id) else add(id)
                        }
                    },
                )
            }
        }
    }
}

private data class ResolvedEntry(val entry: DiagnoseEntry, val symptom: String, val action: String, val explanation: String)

@Composable
private fun DiagnoseCard(
    resolved: ResolvedEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    val (entry, symptom, action, explanation) = resolved

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .clickable(onClickLabel = symptom, onClick = onToggle),
    ) {
        Box(Modifier.width(4.dp).height(if (expanded) 96.dp else 52.dp).background(entry.stripe.color(colors)))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(text = symptom, style = type.rowLabel, color = colors.ink)
            if (expanded) {
                Text(text = action, style = type.rowLabel, color = colors.ink, modifier = Modifier.padding(top = 6.dp))
                Text(text = explanation, style = type.body, color = colors.dim, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
