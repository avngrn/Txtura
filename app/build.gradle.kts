plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "com.txtura"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.txtura"
        minSdk = 29
        targetSdk = 36
        versionCode = 16
        versionName = "3.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val otaDistributionHash = project.findProperty("OTA_DISTRIBUTION_HASH") as? String ?: "demo_ota_hash_12345"
        val otaProjectId = project.findProperty("OTA_PROJECT_ID") as? String ?: "txtura_ota_project"
        buildConfigField("String", "OTA_DISTRIBUTION_HASH", "\"$otaDistributionHash\"")
        buildConfigField("String", "OTA_PROJECT_ID", "\"$otaProjectId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite)
    implementation(libs.sqlcipher)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}