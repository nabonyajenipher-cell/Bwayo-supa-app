plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.bwayo.superapp"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.bwayo.superapp"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
    }
    signingConfigs {
        create("playUpload") {
            val keyPath = System.getenv("BWAYO_KEYSTORE_PATH")
            if (!keyPath.isNullOrBlank()) {
                storeFile = file(keyPath)
                storePassword = System.getenv("BWAYO_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("BWAYO_KEY_ALIAS")
                keyPassword = System.getenv("BWAYO_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            // The release AAB is signed only after the owner adds the upload-key secrets.
            if (!System.getenv("BWAYO_KEYSTORE_PATH").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("playUpload")
            }
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
