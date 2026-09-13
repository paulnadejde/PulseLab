plugins {
    id("com.android.application")
}

android {
    namespace = "ro.aquanano.pulselab"
    compileSdk = 34

    defaultConfig {
        applicationId = "ro.aquanano.pulselab"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "0.1.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
