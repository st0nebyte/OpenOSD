plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace  = "dev.st0nebyte.openosd"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.st0nebyte.openosd"
        minSdk        = 22
        targetSdk     = 36
        versionCode   = 10
        versionName   = "0.6.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Use a fixed debug keystore so APKs can be updated without uninstall
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled   = false
            signingConfig     = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
}

dependencies {
    // Kotlin Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Unit testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.22")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

// Task to generate docs/index.html from Kotlin source files
tasks.register<Exec>("generateDocs") {
    group = "documentation"
    description = "Generate docs/index.html from Kotlin source (OSDView.kt, AVRState.kt)"

    workingDir = rootProject.projectDir
    commandLine = listOf("python3", "scripts/generate_docs.py")

    // Depend on source files
    inputs.files(
        "app/src/main/java/dev/st0nebyte/openosd/OSDView.kt",
        "app/src/main/java/dev/st0nebyte/openosd/AVRState.kt",
        "docs/index.template.html"
    )
    outputs.file("docs/index.html")
}

// Run generateDocs before building
tasks.named("preBuild") {
    dependsOn("generateDocs")
}
