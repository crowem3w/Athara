/*
 * Root build file. Declares the Android Gradle plugin version for the
 * whole project; the app module below applies it without re-specifying
 * the version.
 */

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
