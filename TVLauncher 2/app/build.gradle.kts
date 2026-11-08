plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Signing: Werte kommen aus Umgebungsvariablen (CI) oder gradle.properties.
// Fehlen sie, wird die Release-APK mit dem Debug-Key signiert (installierbar).
fun signingValue(name: String): String? =
    System.getenv(name) ?: (project.findProperty(name) as String?)

val keystorePath = signingValue("KEYSTORE_PATH")

android {
    namespace = "com.btx.tvlauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.btx.tvlauncher"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (keystorePath != null && file(keystorePath).exists()) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = signingValue("KEYSTORE_PASSWORD")
                keyAlias = signingValue("KEY_ALIAS")
                keyPassword = signingValue("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
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
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
