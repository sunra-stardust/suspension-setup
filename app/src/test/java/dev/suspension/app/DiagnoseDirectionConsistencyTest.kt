package dev.suspension.app

import dev.suspension.app.data.DiagnoseData
import dev.suspension.app.data.RotationDirection
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Change 01 §11 test 2 — `direction == CLOCKWISE` iff the action string says close
 * ("zudrehen" / "Close"), `COUNTER_CLOCKWISE` iff it says open ("aufdrehen" / "Open"),
 * otherwise `NONE`. This is a consistency check between the hand-assigned
 * [dev.suspension.app.data.DiagnoseEntry.direction] and the action text in every language —
 * the app itself never derives direction by parsing strings (§6), this test only guards
 * against the two drifting apart.
 */
class DiagnoseDirectionConsistencyTest {

    private class Verbs(val clockwise: Regex, val counterClockwise: Regex)

    private val verbs = mapOf(
        "de" to Verbs(Regex("zudrehen"), Regex("aufdrehen")),
        "en" to Verbs(Regex("""\bclose\b""", RegexOption.IGNORE_CASE), Regex("""\bopen\b""", RegexOption.IGNORE_CASE)),
    )

    @Test
    fun `every language has direction verbs`() {
        assertTrue(verbs.keys == StringsXmlLoader.languages.keys, "Add rotation verbs for ${StringsXmlLoader.languages.keys - verbs.keys}")
    }

    @Test
    fun `diagnose direction matches the verb in its action text`() {
        val failures = mutableListOf<String>()

        for ((language, strings) in StringsXmlLoader.languages) {
            val languageVerbs = verbs.getValue(language)
            for (entry in DiagnoseData.entries) {
                val resourceName = resourceNameFor(entry.actionResId)
                val actionText = strings[resourceName] ?: error("No $language string for $resourceName")

                val closes = languageVerbs.clockwise.containsMatchIn(actionText)
                val opens = languageVerbs.counterClockwise.containsMatchIn(actionText)

                val expected = when {
                    closes && !opens -> RotationDirection.CLOCKWISE
                    opens && !closes -> RotationDirection.COUNTER_CLOCKWISE
                    !closes && !opens -> null
                    else -> error("[$language] $resourceName says both close and open — ambiguous: \"$actionText\"")
                }

                if (expected != entry.direction) {
                    failures += "[$language] $resourceName: action=\"$actionText\" expected direction=$expected but was ${entry.direction}"
                }
            }
        }

        assertTrue(failures.isEmpty(), "Direction/text mismatches:\n" + failures.joinToString("\n"))
    }

    private fun resourceNameFor(resId: Int): String =
        R.string::class.java.fields.first { it.getInt(null) == resId }.name
}
