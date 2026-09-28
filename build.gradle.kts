// Root build script. Plugins are declared here (apply false) so versions resolve once
// from the version catalog; the app module applies the ones it needs.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}

tasks.register("verify") {
    group = "verification"
    description = "Full check: app unit tests + debug build."
    dependsOn(":app:testDebugUnitTest", ":app:assembleDebug")
}
