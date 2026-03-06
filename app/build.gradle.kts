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
        versionCode   = 4
        versionName   = "4.0"
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

// No external dependencies - pure Android SDK only
dependencies {}
