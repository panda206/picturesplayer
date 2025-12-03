plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("kotlin-kapt") // ✅ 必须有
}

android {
    namespace = "com.example.photo6"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.photo6"
        minSdk = 24
        targetSdk = 34
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
    buildFeatures {
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("org.nanohttpd:nanohttpd:2.3.1")
    // implementation("io.ktor:ktor-server-core:2.3.4")
    // implementation("io.ktor:ktor-server-netty:2.3.4")
    // implementation("io.ktor:ktor-server-call-logging:2.3.4")
    // implementation("io.ktor:ktor-server-content-negotiation:2.3.4")
    // implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
    // implementation("io.ktor:ktor-server-cio:2.3.4")


}