package dev.suspension.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import java.time.LocalDate
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
import dev.suspension.app.care.CareContent
import dev.suspension.app.care.CareFormat
import dev.suspension.app.care.CareReminders
import dev.suspension.app.care.CareRepository
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
import dev.suspension.app.ui.care.CareActions
import dev.suspension.app.ui.care.CareScreen
import dev.suspension.app.ui.care.ServiceEntryScreen
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
    /** Bumped each time a reminder notification asks to open Pflege → Kalender. */
    private val careRequest = mutableIntStateOf(0)

    private fun handleCareIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(CareReminders.EXTRA_OPEN_CARE, false) == true) careRequest.intValue++
        // Debug builds only: run the daily reminder check once, now (acceptance test of the worker).
        if (BuildConfig.DEBUG && intent?.getBooleanExtra(CareReminders.EXTRA_RUN_CHECK, false) == true) CareReminders.runOnce(applicationContext)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCareIntent(intent)
    }
    // The in-app language choice overrides the device language for everything this activity shows.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageStore.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val crashGuard = CrashGuard.from(this)
        if (savedInstanceState == null) handleCareIntent(intent)
        // The daily check lives in WorkManager; re-register it in case the app was updated or data restored.
        if (CareRepository.get(this).state.value.remindersEnabled) CareReminders.schedule(applicationContext)
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
                            AppRoot(updates, careRequest = careRequest.intValue, onLanguageChange = { language ->
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
private fun AppRoot(updates: UpdateViewModel?, careRequest: Int, onLanguageChange: (AppLanguage) -> Unit) {
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

    // Pflege: repository, selected section, the entry screen ("new", "new:<ids>" or "edit:<id>") and a transient message.
    val careRepo = remember { CareRepository.get(context) }
    val care by careRepo.state.collectAsState()
    var careSection by rememberSaveable { mutableStateOf(CareContent.CALENDAR_ID) }
    var careEntry by rememberSaveable { mutableStateOf<String?>(null) }
    var careMessage by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(careRequest) {
        if (careRequest > 0) {
            selectedTab = AppTab.CARE.ordinal
            careSection = CareContent.CALENDAR_ID
            careEntry = null
            overlay = PickerOverlay.NONE
        }
    }

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
                AppTab.CARE -> {
                    val today = LocalDate.now()
                    CareScreen(
                        sectionId = careSection,
                        onSectionChange = { careSection = it },
                        data = care,
                        today = today,
                        actions = CareActions(
                            onPurchaseDate = careRepo::setPurchaseDate,
                            onOdometer = { km -> careRepo.setOdometer(km, today) },
                            onReminders = { on ->
                                careRepo.setReminders(on)
                                if (on) CareReminders.schedule(context.applicationContext) else CareReminders.cancel(context.applicationContext)
                            },
                            onKmPerHour = careRepo::setKmPerHour,
                            onTaskReminder = careRepo::setTaskReminder,
                            onLogService = { ids -> careEntry = if (ids.isEmpty()) "new" else "new:" + ids.joinToString(",") },
                            onEditEntry = { id -> careEntry = "edit:$id" },
                        ),
                    )
                }
            }

            careEntry?.let { key ->
                val today = LocalDate.now()
                val editing = if (key.startsWith("edit:")) care.log.firstOrNull { it.id == key.removePrefix("edit:") } else null
                if (key.startsWith("edit:") && editing == null) {
                    careEntry = null
                } else {
                    ServiceEntryScreen(
                        initial = editing,
                        preselected = if (key.startsWith("new:")) key.removePrefix("new:").split(",") else emptyList(),
                        odometerKm = care.odometerKm,
                        today = today,
                        onSave = { entry ->
                            if (careRepo.saveEntry(entry, today)) {
                                careMessage = context.getString(R.string.care_snack_odometer, CareFormat.km(entry.km))
                            }
                            careEntry = null
                        },
                        onDelete = editing?.let { e -> { careRepo.deleteEntry(e.id); careEntry = null } },
                        onCancel = { careEntry = null },
                    )
                }
            }

            careMessage?.let { message ->
                LaunchedEffect(message) {
                    delay(4000)
                    careMessage = null
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppTheme.colors.ink)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(text = message, style = AppTheme.type.body, color = AppTheme.colors.bg)
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