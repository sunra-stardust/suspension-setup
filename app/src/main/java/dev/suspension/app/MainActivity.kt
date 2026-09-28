package dev.suspension.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import dev.suspension.app.data.Scenario
import dev.suspension.app.data.ValueRepository
import dev.suspension.app.ui.BasicsScreen
import dev.suspension.app.ui.DiagnoseScreen
import dev.suspension.app.ui.SetupScreen
import dev.suspension.app.ui.components.AppTab
import dev.suspension.app.ui.components.BottomNavBar
import dev.suspension.app.ui.theme.AppTheme
import dev.suspension.app.ui.theme.SuspensionSetupTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SuspensionSetupTheme {
                Surface(color = AppTheme.colors.bg, modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val repository = remember { ValueRepository(context) }

    var selectedTab by rememberSaveable { mutableIntStateOf(AppTab.SETUP.ordinal) }
    var selectedScenarioIndex by rememberSaveable { mutableIntStateOf(Scenario.BASIS.index) }
    val selectedScenario = Scenario.ordered[selectedScenarioIndex]
    val tab = AppTab.entries[selectedTab]

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                AppTab.SETUP -> {
                    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                    SetupScreen(
                        repository = repository,
                        selectedScenario = selectedScenario,
                        onScenarioSelected = { selectedScenarioIndex = it.index },
                        listState = listState,
                    )
                }
                AppTab.DIAGNOSE -> {
                    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                    DiagnoseScreen(listState = listState)
                }
                AppTab.BASICS -> {
                    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                    BasicsScreen(
                        repository = repository,
                        listState = listState,
                        onResetDone = { selectedTab = AppTab.SETUP.ordinal },
                    )
                }
            }
        }
        BottomNavBar(selected = tab, onSelect = { selectedTab = it.ordinal })
    }
}
