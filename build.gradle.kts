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
}
