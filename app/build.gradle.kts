import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "2.1.21"
    id("com.google.devtools.ksp") version "2.1.21-2.0.1"
}

android {
    namespace = "com.example.collectalogger2"
    compileSdk = 35

    kotlin {
        jvmToolchain(21)
        sourceSets.all {
            kotlin.srcDir("build/generated/ksp/${name}/kotlin")
        }
    }

    defaultConfig {
        applicationId = "com.example.collectalogger2"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        //load the values from .properties file
        val keystoreFile = project.rootProject.file("api_keys.properties")
        val properties = Properties()
        properties.load(keystoreFile.inputStream())

        //return empty key in case something goes wrong
        val igdbApiKey = properties.getProperty("IGDB_API_KEY") ?: ""
            buildConfigField(
                type = "String",
                name = "IGDB_API_KEY",
                value = igdbApiKey
            )
        //return empty key in case something goes wrong
        val steamApiKey = properties.getProperty("STEAM_API_KEY") ?: ""
        buildConfigField(
            type = "String",
            name = "STEAM_API_KEY",
            value = steamApiKey
        )



        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

configurations.all {
    exclude(group = "com.intellij", module = "annotations")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.material3)

    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    // JSON serialization library, works with the Kotlin serialization plugin
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    // coil for async image loading
    implementation(libs.coil3.coil.compose)
    implementation(libs.coil.network.okhttp)
    // this is for Ktor/calling various non-igdb APIs
    //K-tor
    implementation("io.ktor:ktor-client-android:2.3.4")
    implementation(libs.ktor.client.core.v150)
    implementation(libs.ktor.client.serialization.jvm)
    implementation(libs.ktor.client.logging)
    implementation(libs.androidx.work.runtime.ktx)

    // room persisting
    val roomVersion = "2.7.1" // or latest stable

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // DataStore for settings and such
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.preferences.core)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //// Testing
    // Local Unit Tests (Run on JVM)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3") // For testing ViewModels with Coroutines
    testImplementation("app.cash.turbine:turbine:1.0.0") // Highly recommended for testing StateFlows
    testImplementation("io.mockk:mockk:1.13.8") // For mocking repositories/datasources
    testImplementation("org.slf4j:slf4j-simple:2.0.9")

    // UI Tests (Run on Device/Emulator)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
