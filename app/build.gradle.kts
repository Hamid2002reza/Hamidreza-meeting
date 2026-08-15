plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ir.hamidreza.meeting"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.hamidreza.meeting"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "12.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
    }

    // Optional local release signing. Never commit passwords/keystores.
    val ks = providers.gradleProperty("RELEASE_STORE_FILE").orNull
    val kp = providers.gradleProperty("RELEASE_STORE_PASSWORD").orNull
    val ka = providers.gradleProperty("RELEASE_KEY_ALIAS").orNull
    val kpass = providers.gradleProperty("RELEASE_KEY_PASSWORD").orNull
    if (ks != null && kp != null && ka != null && kpass != null) {
        signingConfigs.create("release") {
            storeFile = file(ks)
            storePassword = kp
            keyAlias = ka
            keyPassword = kpass
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("release")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("com.google.android.material:material:1.12.0")
}
