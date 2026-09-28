package dev.suspension.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.suspension.app.R
import dev.suspension.app.data.Scenario
import dev.suspension.app.ui.theme.AppTheme

enum class AppTab(val labelResId: Int) {
    SETUP(R.string.tab_setup),
    DIAGNOSE(R.string.tab_diagnose),
    BASICS(R.string.tab_basics),
}

@Composable
fun ScenarioTabs(
    scenarios: List<Scenario>,
    selected: Scenario,
    labelFor: @Composable (Scenario) -> String,
    onSelect: (Scenario) -> Unit,
) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    ) {
        items(scenarios) { scenario ->
            val isSelected = scenario == selected
            val label = labelFor(scenario)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) colors.ink else colors.hit)
                    .clickable(onClickLabel = label) { onSelect(scenario) }
                    .semantics { contentDescription = label }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = label,
                    style = type.navLabel,
                    color = if (isSelected) colors.surface else colors.ink,
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    val colors = AppTheme.colors
    val type = AppTheme.type
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface),
    ) {
        AppTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val label = stringResource(tab.labelResId)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clickable(onClickLabel = label) { onSelect(tab) }
                    .semantics { contentDescription = label },
            ) {
                Text(
                    text = label,
                    style = type.navLabel,
                    color = if (isSelected) colors.ink else colors.dim,
                )
            }
        }
    }
}
