package dev.suspension.app

import android.content.Context
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.suspension.app.data.AppLanguage
import dev.suspension.app.data.BikeParts
import dev.suspension.app.data.BikeProfiles
import dev.suspension.app.data.GarageRepository
import dev.suspension.app.data.CUSTOM_ID
import dev.suspension.app.data.LanguageStore
import dev.suspension.app.ui.BasicsScreen
import dev.suspension.app.ui.BikesOverlay
import dev.suspension.app.ui.displayName
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

class MainActivity : ComponentActivity() {
    // The in-app language choice overrides the device language for everything this activity shows.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageStore.wrap(newBase))
    }

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
                            AppRoot(updates, onLanguageChange = { language ->
                                LanguageStore.set(this@MainActivity, language)
                                recreate()
                            })
                        }
                    }
                }
            }
        }
    }
}

private enum class PickerOverlay { NONE, FORK, SHOCK, BIKES }

@Composable
private fun AppRoot(updates: UpdateViewModel?, onLanguageChange: (AppLanguage) -> Unit) {
    val context = LocalContext.current
    val garageRepo = remember { GarageRepository.get(context) }
    val garage by garageRepo.state.collectAsState()
    val bike = garage.selectedBike
    val profile = BikeParts.profile(bike)
    val vorlage = garage.selectedVorlage

    var selectedTab by rememberSaveable { mutableIntStateOf(AppTab.SETUP.ordinal) }
    val tab = AppTab.entries[selectedTab]

    // Custom models without a name get a localized default instead of a blank heading.
    val customForkRaw = BikeParts.customFork(bike, profile)
    val customShockRaw = BikeParts.customShock(bike, profile)
    val customForkName = stringResource(R.string.custom_fork_default_name)
    val customShockName = stringResource(R.string.custom_shock_default_name)
    val fork = BikeParts.fork(bike, profile).let { if (it.isCustom) it.copy(displayName = it.displayName.ifBlank { customForkName }) else it }
    val shock = BikeParts.shock(bike, profile).let { if (it.isCustom) it.copy(displayName = it.displayName.ifBlank { customShockName }) else it }

    var overlay by remember { mutableStateOf(PickerOverlay.NONE) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (updates != null) UpdateBanner(updates)
        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                AppTab.SETUP -> {
                    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                    SetupScreen(
                        vorlagen = garage.vorlagen,
                        selectedVorlage = vorlage,
                        onVorlageSelected = { garageRepo.selectVorlage(it.id) },
                        edits = bike.editsFor(vorlage.id),
                        onEdit = { rowId, edit -> garageRepo.setEdit(bike.id, vorlage.id, rowId, edit) },
                        listState = listState,
                        bike = profile,
                        fork = fork,
                        shock = shock,
                        weightKg = garage.weightKg,
                        onWeightChange = garageRepo::setWeight,
                        tempC = garage.tempC,
                        onTempChange = garageRepo::setTemp,
                        onOpenForkPicker = { overlay = PickerOverlay.FORK },
                        onOpenShockPicker = { overlay = PickerOverlay.SHOCK },
                        bikeName = bike.displayName(),
                        onOpenBikes = { overlay = PickerOverlay.BIKES },
                        onAddVorlage = { name -> garageRepo.addVorlage(name, copyFromId = vorlage.id) },
                        onRenameVorlage = { v, name -> garageRepo.renameVorlage(v.id, name) },
                        onDeleteVorlage = { v -> garageRepo.deleteVorlage(v.id) },
                    )
                }
                AppTab.DIAGNOSE -> {
                    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                    DiagnoseScreen(listState = listState)
                }
                AppTab.BASICS -> {
                    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
                    BasicsScreen(
                        listState = listState,
                        bike = profile,
                        fork = fork,
                        shock = shock,
                        updates = updates,
                        language = remember { LanguageStore.get(context) },
                        onLanguageChange = onLanguageChange,
                        onReset = {
                            garageRepo.resetValues(bike.id)
                            selectedTab = AppTab.SETUP.ordinal
                        },
                    )
                }
            }

            when (overlay) {
                PickerOverlay.FORK -> ForkPickerOverlay(
                    currentForkId = bike.forkId,
                    initialCustomFork = customForkRaw,
                    onSelectCatalog = { selected -> garageRepo.selectFork(bike.id, selected.id) },
                    onSaveCustom = { model -> garageRepo.selectFork(bike.id, CUSTOM_ID, model) },
                    onDismiss = { overlay = PickerOverlay.NONE },
                )
                PickerOverlay.SHOCK -> ShockPickerOverlay(
                    currentShockId = bike.shockId,
                    initialCustomShock = customShockRaw,
                    onSelectCatalog = { selected -> garageRepo.selectShock(bike.id, selected.id) },
                    onSaveCustom = { model -> garageRepo.selectShock(bike.id, CUSTOM_ID, model) },
                    onDismiss = { overlay = PickerOverlay.NONE },
                )
                PickerOverlay.BIKES -> BikesOverlay(
                    garage = garage,
                    onSelect = { garageRepo.selectBike(it.id) },
                    onAddCatalog = { model, size -> garageRepo.addCatalogBike(model, size) },
                    onAdd = { name ->
                        garageRepo.addBike(name, bike, BikeParts.installedSpringLbs(bike, garage.weightKg, garage.tempC))
                    },
                    onCopy = { source, name -> garageRepo.copyBike(source.id, name) },
                    onRename = { target, name -> garageRepo.renameBike(target.id, name) },
                    onDelete = { target -> garageRepo.deleteBike(target.id) },
                    onDismiss = { overlay = PickerOverlay.NONE },
                )
                PickerOverlay.NONE -> Unit
            }
        }
        BottomNavBar(selected = tab, onSelect = { selectedTab = it.ordinal })
    }
}