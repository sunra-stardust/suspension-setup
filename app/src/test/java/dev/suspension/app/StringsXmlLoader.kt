package dev.suspension.app

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Reads the `strings.xml` files directly off disk (no Android resources needed) so JVM unit
 * tests can lint-check string content without Robolectric. Gradle's Test task working directory
 * is the module dir (`app/`) by default; we also walk up a couple of levels defensively in case
 * that ever changes.
 */
object StringsXmlLoader {
    data class Entry(val text: String, val translatable: Boolean)

    /** English, the default language (`values/`), including non-translatable keys. */
    val englishEntries: Map<String, Entry> by lazy { load("values") }

    /** German translation (`values-de/`). */
    val germanEntries: Map<String, Entry> by lazy { load("values-de") }

    /** What an English device shows. */
    val english: Map<String, String> by lazy { englishEntries.mapValues { it.value.text } }

    /** What a German device shows: the translation, falling back to non-translatable defaults. */
    val german: Map<String, String> by lazy { english + germanEntries.mapValues { it.value.text } }

    /** Every language the app ships, by resource-folder qualifier. */
    val languages: Map<String, Map<String, String>> by lazy { mapOf("en" to english, "de" to german) }

    private fun load(folder: String): Map<String, Entry> {
        val relative = "src/main/res/$folder/strings.xml"
        val file = locate(relative) ?: error("Could not locate $relative from ${File(".").absolutePath}")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        val result = LinkedHashMap<String, Entry>()
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val name = node.attributes.getNamedItem("name")?.nodeValue ?: continue
            val translatable = node.attributes.getNamedItem("translatable")?.nodeValue != "false"
            check(name !in result) { "Duplicate string \"$name\" in $relative" }
            result[name] = Entry(node.textContent, translatable)
        }
        return result
    }

    private fun locate(relative: String): File? {
        var dir: File? = File(".").absoluteFile
        repeat(4) {
            if (dir != null) {
                val candidate = File(dir, relative)
                if (candidate.exists()) return candidate
                dir = dir?.parentFile
            }
        }
        return null
    }
}
