import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Accounts use Firebase Authentication. The Google services plugin only applies when you have
// added your own app/google-services.json (git-ignored). Without it, debug builds still compile
// and show "accounts not set up"; release builds refuse to build (see the check at the bottom).
val hasFirebaseConfig = file("google-services.json").exists()
if (hasFirebaseConfig) apply(plugin = "com.google.gms.google-services")

// Release signing: read from keystore.properties (git-ignored). Absent = unsigned release build.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.moneytrack.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.moneytrack.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.2.0"
    }

    signingConfigs {
        if (keystoreProps.containsKey("storeFile")) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Run ./gradlew assembleDebug -PauthEmulator=true to talk to the local Firebase Auth
            // emulator (10.0.2.2:9099) instead of a real Firebase project.
            buildConfigField("boolean", "USE_AUTH_EMULATOR", (project.findProperty("authEmulator") == "true").toString())
        }
        release {
            buildConfigField("boolean", "USE_AUTH_EMULATOR", "false")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        // Firebase exception classes call android.text.TextUtils, which is a stub in JVM tests.
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.core:core-ktx:1.16.0")

    val room = "2.7.2"
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    // Accounts: Firebase Authentication (email + password, password-reset email).
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    testImplementation("junit:junit:4.13.2")
}

// Safety net: never ship a release build whose sign-in screen cannot work.
tasks.matching { it.name in setOf("bundleRelease", "assembleRelease", "packageRelease") }.configureEach {
    doFirst {
        check(hasFirebaseConfig) {
            "app/google-services.json is missing. Create a Firebase project, enable Email/Password " +
                "sign-in, register com.moneytrack.app, and download google-services.json into app/. " +
                "See README > Accounts setup."
        }
    }
}
