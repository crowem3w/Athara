/*
 * :core library module. Shared plumbing for every feature module:
 *  - secure/  Android Keystore (AES-GCM) encrypted key-value store + token storage
 *  - net/     Retrofit/OkHttp client, bearer-token interceptor, refresh-token authenticator, DTOs
 *  - sync/    outbox-based sync engine (server is authoritative) + WorkManager scheduling
 * No UI, no Room: feature modules own their tables and plug in a SyncAdapter.
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.example.core"
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
    // Part of the public API of this module (BackendApi, JsonObject in SyncAdapter, MultipartBody).
    api(libs.retrofit)
    api(libs.okhttp)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.work.runtime.ktx)
}
