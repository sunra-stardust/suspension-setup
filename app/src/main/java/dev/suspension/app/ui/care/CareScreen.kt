package dev.suspension.app.ui.care

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.suspension.app.care.CareContent
import dev.suspension.app.care.CareData
import dev.suspension.app.data.BikeTraits
import dev.suspension.app.ui.components.SectionChips
import java.time.LocalDate

/** What the calendar can ask the host to do; the host owns the repository and the entry overlay. */
class CareActions(
    val onPurchaseDate: (LocalDate?) -> Unit,
    val onOdometer: (Int) -> Unit,
    val onReminders: (Boolean) -> Unit,
    val onKmPerHour: (Int) -> Unit,
    val onTaskReminder: (String, Boolean) -> Unit,
    /** Opens the entry screen; the list preselects tasks. */
    val onLogService: (List<String>) -> Unit,
    val onEditEntry: (String) -> Unit,
)

/** The `Pflege` tab: a chip row and the one selected section (switching resets the scroll position). */
@Composable
fun CareScreen(
    sectionId: String,
    onSectionChange: (String) -> Unit,
    data: CareData,
    today: LocalDate,
    bike: BikeTraits,
    actions: CareActions,
) {
    // Only what fits the selected bike; a section that does not apply loses its chip.
    val sections = remember(bike) { CareContent.forBike(bike) }
    val shownId = sectionId.takeIf { id -> sections.any { it.id == id } } ?: CareContent.CALENDAR_ID
    Column(modifier = Modifier.fillMaxSize()) {
        SectionChips(
            sectionIds = sections.map { it.id },
            selectedId = shownId,
            labelFor = { id -> stringResource(sections.first { it.id == id }.chipLabelRes) },
            onSelect = onSectionChange,
        )
        key(shownId) {
            if (shownId == CareContent.CALENDAR_ID) {
                CalendarSection(data = data, today = today, bike = bike, actions = actions)
            } else {
                StaticSection(sections.first { it.id == shownId })
            }
        }
    }
}
