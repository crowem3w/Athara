/*
 * App module. Uses Jetpack Compose for UI.
 *
 * No separate org.jetbrains.kotlin.android plugin is applied: AGP 9's
 * built-in Kotlin support handles Kotlin sources automatically, and
 * errors out if that plugin is applied alongside it. The Compose
 * compiler, however, is a distinct Kotlin compiler plugin
 * (org.jetbrains.kotlin.plugin.compose) and must still be applied
 * explicitly, pinned to a Kotlin version compatible with AGP 9's
 * bundled Kotlin compiler.
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.example.test"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.example.test"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Three environments. Each needs its own backend and (for Google sign-in) its own Android OAuth
    // client, because the staging build has a different package name.
    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:8080/\"") // emulator -> host machine
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "true")
        }
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            // TODO: replace with the staging upload keystore; debug signing keeps it installable meanwhile.
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release") // library modules only define debug/release
            buildConfigField("String", "API_BASE_URL", "\"https://api.staging.example.org/\"") // TODO: real URL
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "false")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api.example.org/\"") // TODO: real URL
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "false")
        }
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
    implementation(project(":core"))
    implementation(project(":onboarding"))
    implementation(project(":authentication"))
    implementation(project(":personal"))
    implementation(project(":business"))
    implementation(project(":organization"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
}
