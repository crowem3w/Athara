/*
 * Root settings file. Updated to add the repositories AGP needs
 * (Google's Maven repo for the Android Gradle plugin + AndroidX artifacts).
 */

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ahtara-client"

// Physical layout follows documentation/MODULE_STRUCTURE.txt. Gradle project names
// (":network", ":personal", ...) stay flat so `project(":x")` dependencies are unchanged;
// only the directory each project lives in is mapped here.
val modulePaths = mapOf(
    // src/core: foundational infrastructure, no account-specific logic
    "secure-storage" to "src/core/secure-storage",
    "network" to "src/core/network",
    "sync-engine" to "src/core/sync-engine",
    "identity-api" to "src/core/identity-api",
    // src/shared: reusable resources consumed by several modules
    "design-system" to "src/shared/design-system",
    "authentication" to "src/shared/authentication",
    // src/modules: one self-contained module per account type
    "personal" to "src/modules/personal",
    "business" to "src/modules/business",
    "organization" to "src/modules/organization",
    // src/app: composition, initialization, navigation between modules
    "app" to "src/app/application",
    "onboarding" to "src/app/onboarding",
)

modulePaths.forEach { (name, path) ->
    include(name)
    project(":$name").projectDir = file(path)
}
