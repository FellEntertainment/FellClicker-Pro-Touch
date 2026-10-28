plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.fellentertainment.fellclickerprotouch"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fellentertainment.fellclickerprotouch"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
        setProperty("archivesBaseName", "FellClicker-Pro-Touch-v$versionName")
    }

    signingConfigs {
        create("release") {
            storeFile = file("fell-release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "FellClicker2026"
            keyAlias = System.getenv("KEY_ALIAS") ?: "fellkey"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "FellClicker2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}