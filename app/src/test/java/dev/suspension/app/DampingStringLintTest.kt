package dev.suspension.app

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Change 01 §11 test 3 — no string resource used on a damping row, or in the Diagnose list,
 * may contain quantity vocabulary ("plus", "minus", "erhöhen"/"verringern",
 * "increase"/"decrease"): those belong to `+`/`−` rows (§3) and are ambiguous once a row is
 * rotation-controlled. Checked in every shipped language.
 */
class DampingStringLintTest {

    private val dampingRowResourceNames = listOf(
        "label_lsc", "label_hsc", "label_lsr", "label_hsr", "label_rebound",
        "hint_f_lsc", "hint_f_hsc", "hint_f_lsr", "hint_f_hsr", "hint_f_reb_single",
        "hint_f_lsr_chart", "hint_f_hsr_chart",
        "hint_s_lsc", "hint_s_hsc", "hint_s_lsr", "hint_s_hsr", "hint_s_reb_single",
        "deviation_clicks_closer#one", "deviation_clicks_closer#other",
        "deviation_clicks_opener#one", "deviation_clicks_opener#other",
        "reference_maker", "reference_start",
    )

    private val bannedWords = listOf("plus", "minus", "erhöhen", "verringern", "increase", "decrease")

    @Test
    fun `damping row strings avoid quantity vocabulary`() {
        for ((language, strings) in StringsXmlLoader.languages) {
            checkNoBannedWords(language, dampingRowResourceNames.associateWith { strings.getValue(it) })
        }
    }

    @Test
    fun `diagnose list strings avoid quantity vocabulary`() {
        for ((language, strings) in StringsXmlLoader.languages) {
            val diagnoseEntries = strings.filterKeys {
                it.startsWith("diag_symptom_") || it.startsWith("diag_action_") || it.startsWith("diag_explanation_")
            }
            assertTrue(diagnoseEntries.isNotEmpty(), "Expected to find diag_* string resources ($language)")
            checkNoBannedWords(language, diagnoseEntries)
        }
    }

    private fun checkNoBannedWords(language: String, entries: Map<String, String>) {
        val violations = mutableListOf<String>()
        for ((name, text) in entries) {
            val lower = text.lowercase()
            for (word in bannedWords) {
                if (lower.contains(word)) {
                    violations += "[$language] $name contains \"$word\": \"$text\""
                }
            }
        }
        assertTrue(violations.isEmpty(), "Banned quantity vocabulary found:\n" + violations.joinToString("\n"))
    }
}
