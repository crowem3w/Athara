/*
 * Authentication library module. Hosts AuthenticationActivity, the sign-up / setup screens,
 * resources (all prefixed "authentication_") and a backend-backed (REST + tokens) AuthRepository.
 * The contract (AuthRepository, AuthUser, AccountType...) lives in :identity-api and the theme in
 * :design-system, so the implementation can be swapped (see Authentication.install) without
 * touching :app or the feature modules.
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.example.authentication"
    compileSdk = 37

    defaultConfig {
        minSdk = 30
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(project(":identity-api"))
    implementation(project(":design-system"))
    implementation(project(":network"))
    implementation(project(":secure-storage"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.googleid)
    debugImplementation(libs.androidx.ui.tooling)
}
