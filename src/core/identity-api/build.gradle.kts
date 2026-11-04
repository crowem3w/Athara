/*
 * :identity-api library module. The contract between the authentication implementation and
 * everything else: AuthRepository, AuthUser, AccountType, PersonalSetup, PhoneVerifier and the
 * Identity entry point. No Compose, no networking. Resources are prefixed "identityapi_".
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "org.example.core.identityapi"
    compileSdk = 37

    defaultConfig {
        minSdk = 30
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// Binary images (webp, png) live in client/assets/images, not in src/main/res.
// androidComponents is AGP's stable variant API; the old sourceSets DSL fails on AGP 9.
androidComponents {
    val imagesDir = rootProject.file("assets/images/identity-api").absolutePath
    onVariants { variant ->
        variant.sources.res?.addStaticSourceDirectory(imagesDir)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    // StateFlow is part of AuthRepository's public API.
    api(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
}
