plugins {
    // AGP 9 provides built-in Kotlin, so we do NOT apply kotlin-android.
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// Version, set by CI via -P flags (see .github/workflows/pipeline.yml): code = commit count on main.
val appVersionName = (project.findProperty("versionName") as String?) ?: "0.0.0-dev"
val appVersionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 1

// CI decodes the keystore and sets KEYSTORE_FILE; locally it's absent → release falls back to debug signing.
val releaseKeystore: String? = System.getenv("KEYSTORE_FILE")

android {
    namespace = "dev.suspension.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.suspension.app"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    flavorDimensions += "distribution"
    productFlavors {
        // Sideloaded from GitHub Releases and updates itself (OTA). Google Play forbids this.
        create("github") {
            dimension = "distribution"
            buildConfigField("String", "UPDATE_REPO", "\"sunra-stardust/suspension-setup\"")
        }
        // Google Play build: no self-updater and no install permission — Play delivers updates.
        create("play") {
            dimension = "distribution"
            buildConfigField("String", "UPDATE_REPO", "\"\"")
        }
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the release key in CI; debug-signed locally so assembleRelease still runs.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.useJUnitPlatform() }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.org.json)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.vintage.engine)
}
