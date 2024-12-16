plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.zafaconapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.zafaconapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core AndroidX dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // JmDNS for mDNS
    implementation("org.jmdns:jmdns:3.6.0")

    // WorkManager for background tasks
    implementation(libs.androidx.work.runtime.ktx)

    // Compose dependencies
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Kotlin standard library
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.20")

    // OkHttp for HTTP requests
    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    // Preferences for settings screen
    implementation("androidx.preference:preference-ktx:1.2.1")

    // WorkManager para AndroidX
    implementation("androidx.work:work-runtime-ktx:2.8.1")

    // Additional dependencies for resolving issues
    implementation("androidx.fragment:fragment-ktx:1.6.1") // Para fragmentos como PreferenceFragmentCompat
    implementation("androidx.appcompat:appcompat:1.6.1")  // Para actividades compatibles
    implementation("androidx.annotation:annotation:1.6.0") // Manejo de anotaciones en Android
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1") // Para ViewModel y ciclo de vida
    implementation("androidx.core:core-ktx:1.12.0") // Compatibilidad con funciones de Android modernas
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
