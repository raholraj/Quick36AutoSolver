plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.quick36.autosolver"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.quick36.autosolver"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // OCR fallback path (only used if node-tree read fails / text is canvas-drawn)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // Coroutines for background processing off the accessibility event thread
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
