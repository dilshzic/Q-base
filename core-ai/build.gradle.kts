plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.algorithmx.q_base.core_ai"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.7.0-alpha03")
    implementation("com.google.android.material:material:1.11.0")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-compiler:2.59.2")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // Datastore
    implementation("androidx.datastore:datastore-preferences:1.1.7")

    // AI
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
}
