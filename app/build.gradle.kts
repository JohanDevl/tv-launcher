plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.johan.tvlauncher"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.johan.tvlauncher"
        minSdk = 28
        targetSdk = 35
        versionName = "0.1.1"
        // 1.2.3 -> 10203: always increases with the semantic version, never edited by hand.
        val (major, minor, patch) = requireNotNull(Regex("""(\d+)\.(\d{1,2})\.(\d{1,2})""").matchEntire(versionName!!)) {
            "versionName must be MAJOR.MINOR.PATCH with MINOR and PATCH below 100"
        }.destructured
        versionCode = major.toInt() * 10000 + minor.toInt() * 100 + patch.toInt()
        ndk { abiFilters += setOf("armeabi-v7a", "arm64-v8a") }
    }

    signingConfigs {
        // Committed on purpose (private repo): every build, from any clone or tag,
        // must carry the same signature so versions can be installed over each other.
        create("release") {
            storeFile = rootProject.file("keystore/release.jks")
            storePassword = "tvlauncher"
            keyAlias = "tvlauncher"
            keyPassword = "tvlauncher"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("release")
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
