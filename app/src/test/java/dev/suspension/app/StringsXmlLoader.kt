package dev.suspension.app

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Reads `res/values/strings.xml` directly off disk (no Android resources needed) so JVM unit
 * tests can lint-check string content without Robolectric. Gradle's Test task working directory
 * is the module dir (`app/`) by default; we also walk up a couple of levels defensively in case
 * that ever changes.
 */
object StringsXmlLoader {
    private const val RELATIVE_PATH = "src/main/res/values/strings.xml"

    val all: Map<String, String> by lazy { load() }

    private fun load(): Map<String, String> {
        val file = locate() ?: error("Could not locate strings.xml (looked for $RELATIVE_PATH from ${File(".").absolutePath})")
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        val result = LinkedHashMap<String, String>()
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            val name = node.attributes.getNamedItem("name")?.nodeValue ?: continue
            result[name] = node.textContent
        }
        return result
    }

    private fun locate(): File? {
        var dir: File? = File(".").absoluteFile
        repeat(4) {
            if (dir != null) {
                val candidate = File(dir, RELATIVE_PATH)
                if (candidate.exists()) return candidate
                dir = dir?.parentFile
            }
        }
        return null
    }
}
