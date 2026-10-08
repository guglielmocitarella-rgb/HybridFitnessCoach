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
        versionCode = 95
        versionName = "0.9.5"
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
    implementation("androidx.health.connect:connect-client:1.1.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}


val canonicalV3 = rootProject.layout.projectDirectory.file("exercise-db-canonical/exercise-db-v3-9r-b-candidate.json")

abstract class GenerateCanonicalV3Asset : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceJson: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val source = sourceJson.get().asFile
        check(source.isFile) { "Required canonical V3 JSON missing: $source" }
        check(source.length() == 19050937L) {
            "Canonical V3 JSON size mismatch: ${source.length()} bytes, expected 19050937"
        }
        val target = outputDirectory.get().file("exercise-db-v3/exercises.json").asFile
        target.parentFile.mkdirs()
        source.copyTo(target, overwrite = true)
    }
}

val generateCanonicalV3Asset = tasks.register<GenerateCanonicalV3Asset>("generateCanonicalV3Asset") {
    sourceJson.set(canonicalV3)
    outputDirectory.set(layout.buildDirectory.dir("generated/assets/canonicalV3"))
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            generateCanonicalV3Asset,
            GenerateCanonicalV3Asset::outputDirectory
        )
    }
}

tasks.withType<Test>().configureEach {
    dependsOn(generateCanonicalV3Asset)
    systemProperty(
        "hfc.v3.asset",
        layout.buildDirectory.file("generated/assets/canonicalV3/exercise-db-v3/exercises.json").get().asFile.absolutePath
    )
    if (name == "testDebugUnitTest") {
        dependsOn("packageDebug")
        systemProperty(
            "hfc.v3.apk",
            layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile.absolutePath
        )
    }
}
