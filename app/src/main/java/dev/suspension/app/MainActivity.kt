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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.suspension.app.data.BikeProfiles
import dev.suspension.app.data.CUSTOM_ID
import dev.suspension.app.data.ComponentCatalog
import dev.suspension.app.data.DEFAULT_TEMP_C
import dev.suspension.app.data.DEFAULT_WEIGHT_KG
import dev.suspension.app.data.Scenario
import dev.suspension.app.data.SettingsRepository
import dev.suspension.app.data.ValueRepository
import dev.suspension.app.ui.BasicsScreen
import dev.suspension.app.ui.DiagnoseScreen
import dev.suspension.app.ui.ForkPickerOverlay
import dev.suspension.app.ui.SetupScreen
import dev.suspension.app.safety.CrashGuard
import dev.suspension.app.safety.CrashLoopPolicy
import dev.suspension.app.ui.SafeModeScreen
import dev.suspension.app.ui.ShockPickerOverlay
import dev.suspension.app.ui.UpdateBanner
import dev.suspension.app.ui.components.AppTab
import dev.suspension.app.ui.components.BottomNavBar
import dev.suspension.app.ui.theme.AppTheme
import dev.suspension.app.ui.theme.SuspensionSetupTheme
import dev.suspension.app.update.Distribution
import dev.suspension.app.update.UpdateViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val crashGuard = CrashGuard.from(this)
        val updater = Distribution.updater(applicationContext)
        val storeUrl = Distribution.storeUrl(applicationContext)
        val updatePrefs = getSharedPreferences("updates", MODE_PRIVATE)
        setContent {
            SuspensionSetupTheme {
                Surface(color = AppTheme.colors.bg, modifier = Modifier.fillMaxSize()) {
                    val updates = updater?.let { viewModel { UpdateViewModel(it, updatePrefs) } }
                    var safeMode by rememberSaveable { mutableStateOf(crashGuard.shouldEnterSafeMode()) }
                    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                        if (safeMode) {
                            SafeModeScreen(updates = updates, storeUrl = storeUrl, onContinue = {
                                crashGuard.clear()
                                safeMode = false
                            })
                        } else {
                            LaunchedEffect(Unit) {
                                delay(CrashLoopPolicy.HEALTHY_AFTER_MS)
                                crashGuard.clear()
                            }
                            LaunchedEffect(updates) { updates?.autoCheck() }
                            AppRoot(updates)
                        }
                    }
                }
            }
        }
    }
}

private enum class PickerOverlay { NONE, FORK, SHOCK }

@Composable
private fun AppRoot(updates: UpdateViewModel?) {
    val context = LocalContext.current
    val bike = BikeProfiles.current
    val repository = remember { ValueRepository(context) }
    val settings = remember { SettingsRepository(context, bike) }
    val scope = rememberCoroutineScope()

    var selectedTab by rememberSaveable { mutableIntStateOf(AppTab.SETUP.ordinal) }
    var selectedScenarioIndex by rememberSaveable { mutableIntStateOf(Scenario.BASIS.index) }
    val selectedScenario = Scenario.ordered.getOrElse(selectedScenarioIndex) { Scenario.BASIS }
    val tab = AppTab.entries[selectedTab]

    // Kalt/Warm are no longer scenarios (temperature is a setting) — drop their stored overrides.
    LaunchedEffect(Unit) { repository.dropScenarioIndicesFrom(Scenario.count) }

    val stockFork = ComponentCatalog.forkById(bike.stockForkId) ?: ComponentCatalog.forks.first()
    val stockShock = ComponentCatalog.shockById(bike.stockShockId) ?: ComponentCatalog.shocks.first()

    val weightKg by settings.weightKg.collectAsState(initial = DEFAULT_WEIGHT_KG)
    val tempC by settings.tempC.collectAsState(initial = DEFAULT_TEMP_C)
    val forkId by settings.forkId.collectAsState(initial = bike.stockForkId)
    val shockId by settings.shockId.collectAsState(initial = bike.stockShockId)
    val customForkRaw by settings.customFork.collectAsState(initial = stockFork.copy(id = CUSTOM_ID, displayName = ""))
    val customShockRaw by settings.customShock.collectAsState(initial = stockShock.copy(id = CUSTOM_ID, displayName = ""))

    // Custom models without a name get a localized default instead of a blank heading.
    val customFork = customForkRaw.copy(displayName = customForkRaw.displayName.ifBlank { stringResource(R.string.custom_fork_default_name) })
    val customShock = customShockRaw.copy(displayName = customShockRaw.displayName.ifBlank { stringResource(R.string.custom_shock_default_name) })

    // A stored id that's no longer in the catalog (e.g. a removed model) falls back to the bike's stock part.
    val fork = if (forkId == CUSTOM_ID) customFork else (ComponentCatalog.forkById(forkId) ?: stockFork)
    val shock = if (shockId == CUSTOM_ID) customShock else (ComponentCatalog.shockById(shockId) ?: stockShock)

    var overlay by remember { mutableStateOf(PickerOverlay.NONE) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (updates != null) UpdateBanner(updates)
        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                AppTab.SETUP -> {
                    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                    SetupScreen(
                        repository = repository,
                        selectedScenario = selectedScenario,
                        onScenarioSelected = { selectedScenarioIndex = it.index },
                        listState = listState,
                        bike = bike,
                        fork = fork,
                        shock = shock,
                        weightKg = weightKg,
                        onWeightChange = { scope.launch { settings.setWeightKg(it) } },
                        tempC = tempC,
                        onTempChange = { scope.launch { settings.setTempC(it) } },
                        onOpenForkPicker = { overlay = PickerOverlay.FORK },
                        onOpenShockPicker = { overlay = PickerOverlay.SHOCK },
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
                        bike = bike,
                        fork = fork,
                        shock = shock,
                        updates = updates,
                        onResetDone = { selectedTab = AppTab.SETUP.ordinal },
                    )
                }
            }

            when (overlay) {
                PickerOverlay.FORK -> ForkPickerOverlay(
                    currentForkId = forkId,
                    initialCustomFork = customForkRaw,
                    onSelectCatalog = { selected ->
                        scope.launch {
                            settings.selectFork(selected.id)
                            repository.clearKeysWithPrefix("f_")
                        }
                    },
                    onSaveCustom = { model ->
                        scope.launch {
                            settings.setCustomFork(model)
                            settings.selectFork(CUSTOM_ID)
                            repository.clearKeysWithPrefix("f_")
                        }
                    },
                    onDismiss = { overlay = PickerOverlay.NONE },
                )
                PickerOverlay.SHOCK -> ShockPickerOverlay(
                    currentShockId = shockId,
                    initialCustomShock = customShockRaw,
                    onSelectCatalog = { selected ->
                        scope.launch {
                            settings.selectShock(selected.id)
                            repository.clearKeysWithPrefix("s_")
                        }
                    },
                    onSaveCustom = { model ->
                        scope.launch {
                            settings.setCustomShock(model)
                            settings.selectShock(CUSTOM_ID)
                            repository.clearKeysWithPrefix("s_")
                        }
                    },
                    onDismiss = { overlay = PickerOverlay.NONE },
                )
                PickerOverlay.NONE -> Unit
            }
        }
        BottomNavBar(selected = tab, onSelect = { selectedTab = it.ordinal })
    }
}
