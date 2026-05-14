
plugins {
    kotlin("plugin.serialization") version "2.2.0"
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)

    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}


android {
    namespace = "com.simulatedtez.gochat"
    compileSdk = 36

    android.buildFeatures.buildConfig = true

    defaultConfig {
        multiDexEnabled = true
        applicationId = "com.simulatedtez.gochat"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "AUTH_BASE_URL", "\"http://192.168.0.3:50051\"")
            buildConfigField("String", "CHAT_HISTORY_BASE_URL", "\"http://192.168.0.3:50053\"")
            buildConfigField("String", "WEBSOCKET_BASE_URL", "\"ws://192.168.0.3:50053\"")

        }
        release {
            buildConfigField("String", "AUTH_BASE_URL", "\"http://gochat.com\"")
            buildConfigField("String", "CHAT_HISTORY_BASE_URL", "\"http://gochat.com\"")
            buildConfigField("String", "WEBSOCKET_BASE_URL", "\"ws://gochat.com\"")
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.material.icons.extended)

    implementation(platform("androidx.compose:compose-bom:2024.04.01"))

    implementation(libs.androidx.multidex)

    // For Kotlin users
    implementation (libs.androidx.navigation.fragment.ktx)
    implementation (libs.androidx.navigation.ui.ktx)

    // If you're using Compose
    implementation (libs.androidx.navigation.compose)

    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Room components
    implementation(libs.androidx.room.runtime)
    //ksp(libs.androidx.room.compiler)
    // Kotlin coroutines support
    implementation(libs.androidx.room.ktx)
    // for annotation processing
    implementation(libs.androidx.lifecycle.runtime.ktx.v270)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.security.crypto)
    implementation(libs.napier)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(project.files("libs/chat-library-2.0.jar"))
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.runtime.livedata)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}