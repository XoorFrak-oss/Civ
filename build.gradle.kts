plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.civsim"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.civsim"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release { isMinifyEnabled = false }
        debug  { isMinifyEnabled = false }
    }

    // IMPORTANT pour Compose + AGP 8.5
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures { compose = true }

    // OK pour Compose 1.6.x
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }

    // (facultatif mais évite certains warnings AAR)
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // BOM Compose 1.6.6
    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3:1.2.1") // version explicite = +stable
    implementation("androidx.activity:activity-compose:1.9.0")

    // Base AndroidX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
}

// Dépôts aussi au niveau module (certains runners en ont besoin)
repositories {
    google()
    mavenCentral()
}