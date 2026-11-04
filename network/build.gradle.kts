/*
 * :network library module. Retrofit/OkHttp client, bearer-token interceptor, single-flight
 * refresh-token authenticator, ApiResult, the generic sync endpoints (SyncApi) and the Core wiring.
 * Feature modules declare their own Retrofit interfaces and create them with Core.create(...).
 * No UI, no Room.
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.example.network"
    compileSdk = 37

    defaultConfig {
        minSdk = 30
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
    // TokenStore / SecureStore appear in Core's public API.
    api(project(":secure-storage"))
    // Part of the public API of this module (Retrofit services, JsonObject in DTOs, MultipartBody).
    api(libs.retrofit)
    api(libs.okhttp)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging)
}
