package dev.suspension.app

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import dev.suspension.app.data.AppLanguage
import dev.suspension.app.data.LanguageStore
import dev.suspension.app.safety.CrashGuard
import dev.suspension.app.update.UpdatePolicy
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Starts the real app (JVM, Robolectric) and walks every screen. Runs on every push, so a
 * release that can't start or render a tab never ships.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppSmokeTest {

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun str(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    /** The Setup list (the Vorlage tabs scroll horizontally). */
    private val verticalList = hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    @Test
    fun `starts on the setup screen with the shock group`() {
        compose.onNodeWithText(str(R.string.app_header_title, str(R.string.bike_level_rr_name)), substring = true).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.scenario_downhill)).assertIsDisplayed()
    }

    @Test
    fun `closing a damping circuit shows the deviation from Fox`() {
        val forkLsc = "${str(R.string.prefix_gabel)} ${str(R.string.label_lsc)}"
        val closeLabel = str(R.string.rotation_cw_description, forkLsc, str(R.string.rotation_caption_firmer))
        compose.onNode(verticalList).performScrollToNode(hasContentDescription(closeLabel))
        compose.onNodeWithContentDescription(closeLabel).performClick()

        val reference = str(R.string.reference_maker, "Fox")
        val expected = compose.activity.resources.getQuantityString(R.plurals.deviation_clicks_closer, 1, 1, reference)
        compose.onNode(verticalList).performScrollToNode(hasText(expected, substring = true))
        compose.onNodeWithText(expected, substring = true).assertIsDisplayed()
    }

    @Test
    fun `a new preset appears as a tab and can be deleted again`() {
        compose.onNodeWithContentDescription(str(R.string.vorlage_new)).performClick()
        compose.onNodeWithContentDescription(str(R.string.name_label)).performTextInput("Nasse Wurzeln")
        compose.onNodeWithText(str(R.string.action_save)).performClick()
        compose.onNodeWithText("Nasse Wurzeln").assertIsDisplayed()

        compose.onNodeWithText(str(R.string.action_delete)).performClick()
        compose.onAllNodesWithText(str(R.string.action_delete)).onLast().performClick()
        compose.onAllNodesWithText("Nasse Wurzeln").assertCountEquals(0)
    }

    @Test
    fun `a copied bike can be switched to`() {
        val original = str(R.string.bike_level_rr_name)
        compose.onNodeWithText(str(R.string.app_header_title, original), substring = true).performClick()
        compose.onNodeWithText(str(R.string.action_copy)).performClick()
        compose.onNodeWithText(str(R.string.action_save)).performClick()
        compose.onNodeWithText(str(R.string.picker_close)).performClick()

        val copyName = str(R.string.bike_copy_name, original)
        compose.onNodeWithText(str(R.string.app_header_title, copyName), substring = true).assertIsDisplayed()

        compose.onNodeWithText(str(R.string.app_header_title, copyName), substring = true).performClick()
        compose.onNodeWithText(original).performClick()
        compose.onNodeWithText(str(R.string.app_header_title, original), substring = true).assertIsDisplayed()
    }

    @Test
    fun `a new bike is picked from the catalog on its stock parts`() {
        compose.onNodeWithText(str(R.string.app_header_title, str(R.string.bike_level_rr_name)), substring = true).performClick()
        compose.onNodeWithText(str(R.string.bike_new)).performClick()
        compose.onNodeWithText(str(R.string.bike_catalog_title)).assertIsDisplayed()
        compose.onNodeWithText("Mondraker").performClick()
        compose.onNodeWithText("Mondraker Level RR 2026").performClick()
        compose.onNodeWithText("L").performClick()

        compose.onNodeWithText(str(R.string.app_header_title, "Mondraker Level RR 2026"), substring = true).assertIsDisplayed()
    }

    @Test
    fun `a bike that isn't listed can still be added by name`() {
        compose.onNodeWithText(str(R.string.app_header_title, str(R.string.bike_level_rr_name)), substring = true).performClick()
        compose.onNodeWithText(str(R.string.bike_new)).performClick()
        compose.onNodeWithContentDescription(str(R.string.bike_catalog_search_label)).performTextInput("Canyon Strive")
        compose.onNodeWithText(str(R.string.bike_catalog_no_match)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.bike_catalog_not_found)).performClick()
        compose.onNodeWithContentDescription(str(R.string.name_label)).performTextInput("Strive")
        compose.onNodeWithText(str(R.string.action_save)).performClick()
        compose.onNodeWithText(str(R.string.picker_close)).performClick()

        compose.onNodeWithText(str(R.string.app_header_title, "Strive"), substring = true).assertIsDisplayed()
    }

    @Test
    fun `every tab renders`() {
        compose.onNodeWithText(str(R.string.tab_diagnose)).performClick()
        compose.onAllNodesWithText(str(R.string.diag_symptom_1)).onFirst().assertIsDisplayed()

        compose.onNodeWithText(str(R.string.tab_basics)).performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(str(R.string.app_version, BuildConfig.VERSION_NAME)))
        compose.onNodeWithText(str(R.string.app_version, BuildConfig.VERSION_NAME)).assertIsDisplayed()

        compose.onNodeWithText(str(R.string.tab_setup)).performClick()
        compose.onNodeWithText(str(R.string.scenario_basis)).assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun noNetwork() {
            UpdatePolicy.autoCheckEnabled = false
        }
    }
}

/** English by default; the language choice on Basics switches the whole UI and is remembered. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LanguageTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun german(id: Int): String {
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.GERMAN) }
        return context.createConfigurationContext(config).getString(id)
    }

    private fun english(id: Int): String {
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.ENGLISH) }
        return context.createConfigurationContext(config).getString(id)
    }

    @Test
    fun `stored German choice starts in German`() {
        UpdatePolicy.autoCheckEnabled = false
        LanguageStore.set(context, AppLanguage.GERMAN)
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText(german(R.string.group_conditions)).assertIsDisplayed()
        }
    }

    @Test
    fun `choosing Deutsch on Basics switches the UI to German`() {
        UpdatePolicy.autoCheckEnabled = false
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText(english(R.string.group_conditions)).assertIsDisplayed()

            compose.onNodeWithText(english(R.string.tab_basics)).performClick()
            compose.onNode(hasScrollAction()).performScrollToNode(hasText(english(R.string.language_german)))
            compose.onNodeWithText(english(R.string.language_german)).performClick()
            compose.waitForIdle()

            assertEquals(AppLanguage.GERMAN, LanguageStore.get(context))
            compose.onNode(hasScrollAction()).performScrollToNode(hasText(german(R.string.basics_language_title)))
            compose.onNodeWithText(german(R.string.basics_language_title)).assertIsDisplayed()
        }
    }
}

/** After repeated crashes the app opens in safe mode, and "start anyway" returns to the normal UI. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SafeModeTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    @Test
    fun `crash loop opens safe mode, start anyway recovers`() {
        UpdatePolicy.autoCheckEnabled = false
        val context = ApplicationProvider.getApplicationContext<Context>()
        CrashGuard.from(context).apply {
            recordCrash()
            recordCrash()
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText(context.getString(R.string.safe_mode_title)).assertIsDisplayed()
            compose.onNodeWithText(context.getString(R.string.safe_mode_continue)).performClick()
            compose.onNodeWithText(context.getString(R.string.tab_basics)).assertIsDisplayed()
        }
    }
}
