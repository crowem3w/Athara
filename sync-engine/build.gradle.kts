/*
 * :sync-engine library module. Outbox-based sync (server is authoritative) plus WorkManager
 * scheduling. Feature modules own their Room tables and plug in a SyncAdapter / TableSync, and
 * expose a SyncInstaller so the app can register them.
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "org.example.syncengine"
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
    // SyncAdapter's signatures use the network DTOs and kotlinx-serialization's JsonObject.
    api(project(":network"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)
}
