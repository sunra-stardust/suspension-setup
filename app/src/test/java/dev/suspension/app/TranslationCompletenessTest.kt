package dev.suspension.app

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Phase 2: English is the default (`values/`), German the translation (`values-de/`). Every
 * translatable key must exist in both, with the same placeholders — otherwise a German device
 * silently shows English, or a format call crashes.
 */
class TranslationCompletenessTest {

    private val english = StringsXmlLoader.englishEntries
    private val german = StringsXmlLoader.germanEntries

    @Test
    fun `every translatable key is translated to German`() {
        val missing = english.filter { it.value.translatable && it.key !in german }.keys
        assertTrue(missing.isEmpty(), "Missing in values-de/strings.xml:\n" + missing.joinToString("\n"))
    }

    @Test
    fun `German has no keys that English lacks`() {
        val extra = german.keys - english.keys
        assertTrue(extra.isEmpty(), "Only in values-de/strings.xml (add to values/ first):\n" + extra.joinToString("\n"))
    }

    @Test
    fun `non-translatable keys are not translated`() {
        val translated = english.filter { !it.value.translatable && it.key in german }.keys
        assertTrue(translated.isEmpty(), "Marked translatable=\"false\" but present in values-de:\n" + translated.joinToString("\n"))
    }

    @Test
    fun `no translation is empty`() {
        val empty = (english + german).filter { it.value.text.isBlank() }.keys
        assertTrue(empty.isEmpty(), "Empty strings:\n" + empty.joinToString("\n"))
    }

    @Test
    fun `placeholders match between languages`() {
        val mismatches = german.mapNotNull { (key, de) ->
            val en = english[key] ?: return@mapNotNull null
            val enArgs = placeholders(en.text)
            val deArgs = placeholders(de.text)
            if (enArgs != deArgs) "$key: en=$enArgs de=$deArgs" else null
        }
        assertTrue(mismatches.isEmpty(), "Placeholder mismatch:\n" + mismatches.joinToString("\n"))
    }

    /** Positional format args (`%1$s`, `%2$d`) and escaped percent signs (`%%`). */
    private fun placeholders(text: String): List<String> =
        Regex("""%(\d+\$[a-z]|%)""").findAll(text).map { it.value }.sorted().toList()
}
