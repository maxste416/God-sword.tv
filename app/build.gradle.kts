// ============================================================================
// CORRECTED build.gradle.kts (App level)
// Location: God'sword.TV/app/build.gradle.kts
// ============================================================================

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")  // ADDED: Required for Glide and Room annotation processing
}

android {
    namespace = "com.godsword.tv"  // FIXED: Changed from com.example.godswordtv
    compileSdk = 35

    defaultConfig {
        applicationId = "com.godsword.tv"  // FIXED: Changed to match namespace
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17  // UPGRADED: Changed from 11 to 17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"  // UPGRADED: Changed from 11 to 17
    }
    
    buildFeatures {
        viewBinding = true  // ADDED: Enable ViewBinding
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.6.1")  // ADDED
    
    // Leanback Library for Android TV
    implementation(libs.androidx.leanback)
    implementation("androidx.leanback:leanback-preference:1.0.0")  // ADDED
    
    // ExoPlayer (Media3) for video playback - CRITICAL: THESE WERE MISSING!
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.2.1")
    
    // Glide for image loading
    implementation(libs.glide)
    kapt("com.github.bumptech.glide:compiler:4.16.0")  // ADDED: Required for Glide
    
    // CardView
    implementation("androidx.cardview:cardview:1.0.0")  // FIXED: Kotlin DSL syntax
    
    // Room Database for favorites
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // Coroutines - ADDED
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Lifecycle - ADDED
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}