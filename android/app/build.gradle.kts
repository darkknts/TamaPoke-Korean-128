plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.tamapoke.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tamapoke.android"
        minSdk = 26
        targetSdk = 28
        versionCode = 2
        versionName = "0.1.1-diagnostic"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
}
