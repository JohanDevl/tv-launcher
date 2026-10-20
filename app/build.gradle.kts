import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release signing comes from keystore.properties (git-ignored, see README "Signing key").
// Without it, release builds are signed with the debug key: fine to try the app, but such an
// APK cannot be installed over one signed with the real key.
val keystoreProperties = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use(::load) }
}

android {
    namespace = "dev.johan.tvlauncher"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.johan.tvlauncher"
        minSdk = 28
        targetSdk = 35
        versionName = "0.2.0"
        // 1.2.3 -> 10203: always increases with the semantic version, never edited by hand.
        val (major, minor, patch) = requireNotNull(Regex("""(\d+)\.(\d{1,2})\.(\d{1,2})""").matchEntire(versionName!!)) {
            "versionName must be MAJOR.MINOR.PATCH with MINOR and PATCH below 100"
        }.destructured
        versionCode = major.toInt() * 10000 + minor.toInt() * 100 + patch.toInt()
        ndk { abiFilters += setOf("armeabi-v7a", "arm64-v8a") }
    }

    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
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

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin", "kotlin-tooling-metadata.json")
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.tv.material)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.palette)
    implementation(libs.profileinstaller)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}
