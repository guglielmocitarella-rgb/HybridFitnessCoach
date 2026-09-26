plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hybridfitnesscoach.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hybridfitnesscoach.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 8
        versionName = "0.8.0"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("HFC_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("HFC_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("HFC_KEY_ALIAS")
                keyPassword = System.getenv("HFC_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
