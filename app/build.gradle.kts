plugins {
    id("com.android.application")
}

val releaseKeystorePath = System.getenv("PULSELAB_KEYSTORE_PATH")
val solarMasterPasswordHash = System.getenv("SOLARITM_MASTER_PASSWORD_HASH") ?: ""

android {
    namespace = "ro.aquanano.pulselab"
    compileSdk = 34

    defaultConfig {
        applicationId = "ro.aquanano.pulselab"
        minSdk = 26
        targetSdk = 34
        versionCode = 34
        versionName = "0.1.33"
        buildConfigField("String", "SOLARITM_MASTER_PASSWORD_HASH",
            "\"${solarMasterPasswordHash}\"")
    }

    signingConfigs {
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = System.getenv("PULSELAB_STORE_PASSWORD")
                keyAlias = System.getenv("PULSELAB_KEY_ALIAS")
                keyPassword = System.getenv("PULSELAB_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseKeystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
