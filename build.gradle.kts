// Root build script. Plugins are declared here (apply false) so versions resolve once
// from the version catalog; the app module applies the ones it needs.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}

tasks.register("verify") {
    group = "verification"
    description = "Full check: unit + UI tests (both flavors) and debug builds. Must pass before every push."
    dependsOn(
        ":app:testGithubDebugUnitTest",
        ":app:testPlayDebugUnitTest",
        ":app:assembleGithubDebug",
        ":app:assemblePlayDebug",
    )
    // Records the tree that passed (committed + uncommitted, .gitignore respected) in
    // build/verify-stamp. The push guard (.claude/hooks/guard.py) compares it with the pushed commit.
    val root = rootDir
    doLast {
        fun git(index: File, vararg args: String): String {
            val process = ProcessBuilder(listOf("git") + args).directory(root).redirectErrorStream(true)
                .apply { environment()["GIT_INDEX_FILE"] = index.absolutePath }.start()
            val out = process.inputStream.bufferedReader().readText().trim()
            return if (process.waitFor() == 0) out else ""
        }
        val index = File(root, "build/verify-index").apply { parentFile.mkdirs(); delete() }
        git(index, "read-tree", "HEAD")
        git(index, "add", "-A")
        val tree = git(index, "write-tree")
        index.delete()
        val stamp = File(root, "build/verify-stamp")
        if (tree.matches(Regex("[0-9a-f]{40,64}"))) stamp.writeText("$tree\n") else stamp.delete()
    }
}
