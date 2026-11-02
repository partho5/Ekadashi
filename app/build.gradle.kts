plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.jovoc.ekadashi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jovoc.ekadashi"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0"

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    sourceSets {
        getByName("main") {
            assets.srcDir("../data")
        }
    }

    signingConfigs {
        getByName("debug") {
            enableV1Signing = true
            enableV2Signing = true
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
        }
    }

    buildFeatures {
        buildConfig = false
        aidl = false
        renderScript = false
        resValues = false
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    androidResources {
        localeFilters += listOf("en", "bn", "hi")
    }

    // The app switches language in-app, so Play must ship all languages
    // instead of only the device language.
    bundle {
        language {
            enableSplit = false
        }
    }

    packaging {
        resources {
            excludes += listOf(
                "kotlin/**",
                "META-INF/*.kotlin_module",
                "META-INF/*.version",
                "DebugProbesKt.bin"
            )
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
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
