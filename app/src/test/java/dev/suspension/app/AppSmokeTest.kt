package dev.suspension.app

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import dev.suspension.app.safety.CrashGuard
import dev.suspension.app.update.UpdatePolicy
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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

    @Test
    fun `starts on the setup screen with the shock group`() {
        compose.onNodeWithText(str(R.string.app_header_title, str(R.string.bike_level_rr_name))).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.scenario_downhill)).assertIsDisplayed()
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
