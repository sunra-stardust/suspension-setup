package dev.suspension.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onFirst
import dev.suspension.app.care.CareRepository
import dev.suspension.app.update.UpdatePolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The Pflege tab on a (Robolectric) device: chips, static sections, odometer, and logging a service. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CareSmokeTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() = CareRepository.forgetInstance()

    private fun str(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private val verticalList = hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    private fun openCare() {
        compose.onNodeWithText(str(R.string.tab_care)).performClick()
    }

    /** The entry screen scrolls: bring its Save button into view before tapping it. */
    private fun clickSave() {
        compose.onAllNodesWithText(str(R.string.action_save)).onLast().performScrollTo().performClick()
    }

    private fun scrollTo(text: String) {
        compose.onNode(verticalList).performScrollToNode(hasText(text, substring = true))
    }

    @Test
    fun `the tab opens on the calendar with seven chips and nothing due`() {
        openCare()
        compose.onNodeWithText(str(R.string.care_due_title)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.care_due_empty), substring = true).assertIsDisplayed()
        for (chip in listOf(
            R.string.care_chip_calendar, R.string.care_chip_after_ride, R.string.care_chip_products,
            R.string.care_chip_protection, R.string.care_chip_battery, R.string.care_chip_cells, R.string.care_chip_parts,
        )) {
            compose.onNodeWithContentDescription(str(chip)).assertExists()
        }
    }

    @Test
    fun `a static section shows its cards, the card tag once and the footer legend`() {
        openCare()
        compose.onNodeWithContentDescription(str(R.string.care_chip_after_ride)).performClick()
        compose.onNodeWithText(str(R.string.care_after_ride_c1_title)).assertIsDisplayed()
        compose.onAllNodesWithText(str(R.string.care_tag_line, str(R.string.care_tag_faustregel)), substring = true).assertCountEquals(1)
        scrollTo(str(R.string.care_footer_legend))
        compose.onNodeWithText(str(R.string.care_footer_legend)).assertIsDisplayed()
    }

    @Test
    fun `the battery section starts with the Bosch line and tags its rows`() {
        openCare()
        compose.onNodeWithContentDescription(str(R.string.care_chip_battery)).performClick()
        compose.onNodeWithText(str(R.string.care_battery_intro)).assertIsDisplayed()
        scrollTo(str(R.string.care_battery_c2_r1_text))
        compose.onNodeWithText(str(R.string.care_battery_c2_r1_text), substring = true).assertIsDisplayed()
        compose.onAllNodesWithText(str(R.string.care_tag_line, str(R.string.care_tag_hersteller)), substring = true).onFirst().assertExists()
        scrollTo(str(R.string.care_footer_legend))
    }

    @Test
    fun `setting the odometer, a lower value asks first`() {
        openCare()
        scrollTo(str(R.string.care_odometer))
        compose.onNodeWithText(str(R.string.care_odometer)).performClick()
        compose.onNodeWithContentDescription(str(R.string.care_odo_field)).performTextReplacement("320")
        compose.onAllNodesWithText(str(R.string.action_save)).onLast().performClick()
        compose.waitForIdle()
        assertEquals(320, CareRepository.get(compose.activity).state.value.odometerKm)
        scrollTo("320 km")
        compose.onNodeWithText("320 km").assertExists()

        scrollTo(str(R.string.care_odometer))
        compose.onNodeWithText(str(R.string.care_odometer)).performClick()
        compose.onNodeWithContentDescription(str(R.string.care_odo_field)).performTextReplacement("300")
        compose.onAllNodesWithText(str(R.string.action_save)).onLast().performClick()
        compose.onNodeWithText(str(R.string.care_odo_confirm_lower, "300", "320")).assertExists()
        assertEquals(320, CareRepository.get(compose.activity).state.value.odometerKm)
        compose.onNodeWithText(str(R.string.action_cancel)).performClick()
        assertEquals(320, CareRepository.get(compose.activity).state.value.odometerKm)
    }

    @Test
    fun `logging a service from the card puts it in the history and the entry can be removed again`() {
        openCare()
        scrollTo(str(R.string.care_log_service))
        compose.onNodeWithText(str(R.string.care_log_service)).performClick()

        // Nothing chosen and nothing written: the rule is explained.
        clickSave()
        compose.onNodeWithText(str(R.string.care_err_nothing)).assertExists()

        compose.onNodeWithContentDescription(str(R.string.care_field_note)).performTextInput("Gabel beim Händler")
        clickSave()

        scrollTo("Gabel beim Händler")
        compose.onNodeWithText("Gabel beim Händler").assertIsDisplayed()
        assertEquals(1, CareRepository.get(compose.activity).state.value.log.size)

        compose.onNodeWithText("Gabel beim Händler").performClick()
        compose.onNodeWithText(str(R.string.care_entry_edit_title)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.action_delete)).performScrollTo().performClick()
        compose.onNodeWithText(str(R.string.care_entry_delete_confirm)).assertExists()
        compose.onAllNodesWithText(str(R.string.action_delete)).onLast().performClick()
        assertTrue(CareRepository.get(compose.activity).state.value.log.isEmpty())
    }

    @Test
    fun `an overdue task is listed with status in words, and logging it makes it done`() {
        val repo = CareRepository.get(compose.activity)
        repo.setPurchaseDate(LocalDate.now().minusDays(45))
        repo.setOdometer(320, LocalDate.now())
        openCare()

        compose.onAllNodesWithText(str(R.string.care_status_overdue)).onFirst().assertIsDisplayed()
        val name = str(R.string.care_task_first_inspection_name)

        // Expand the task in its group card and log it as done.
        scrollTo(str(R.string.care_group_whole))
        compose.onAllNodes(hasText(name) and hasClickAction()).onFirst().performClick()
        compose.onNodeWithText(str(R.string.care_task_done)).performScrollTo().performClick()
        clickSave()
        compose.waitForIdle()

        assertEquals(listOf("first_inspection"), repo.state.value.log.single().taskIds)
    }
}
