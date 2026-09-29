package dev.suspension.app

import dev.suspension.app.data.DiagnoseData
import dev.suspension.app.data.RotationDirection
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Change 01 §11 test 2 — `direction == CLOCKWISE` iff the action string contains "zudrehen",
 * `COUNTER_CLOCKWISE` iff it contains "aufdrehen", otherwise `NONE`. This is a consistency check
 * between the hand-assigned [dev.suspension.app.data.DiagnoseEntry.direction] and the actual
 * action text in strings.xml — the app itself never derives direction by parsing strings (§6),
 * this test only guards against the two drifting apart.
 */
class DiagnoseDirectionConsistencyTest {

    @Test
    fun `diagnose direction matches the verb in its action text`() {
        val strings = StringsXmlLoader.all
        val failures = mutableListOf<String>()

        for (entry in DiagnoseData.entries) {
            val resourceName = resourceNameFor(entry.actionResId)
            val actionText = strings[resourceName] ?: error("No strings.xml entry for $resourceName")

            val hasZudrehen = actionText.contains("zudrehen")
            val hasAufdrehen = actionText.contains("aufdrehen")

            val expected = when {
                hasZudrehen && !hasAufdrehen -> RotationDirection.CLOCKWISE
                hasAufdrehen && !hasZudrehen -> RotationDirection.COUNTER_CLOCKWISE
                !hasZudrehen && !hasAufdrehen -> null
                else -> error("$resourceName contains both zudrehen and aufdrehen — ambiguous: \"$actionText\"")
            }

            if (expected != entry.direction) {
                failures += "$resourceName: action=\"$actionText\" expected direction=$expected but was ${entry.direction}"
            }
        }

        assertTrue(failures.isEmpty(), "Direction/text mismatches:\n" + failures.joinToString("\n"))
    }

    private fun resourceNameFor(resId: Int): String =
        R.string::class.java.fields.first { it.getInt(null) == resId }.name
}
