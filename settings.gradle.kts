




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

rootProject.name = "ahtara"

// Gradle paths stay flat (":personal"); only the folders are grouped.
val modules = mapOf(
    ":app" to "app",
    ":core" to "core",
    // Account types: one module (and one data layer) each.
    ":personal" to "accounts/personal",
    ":business" to "accounts/business",
    ":organization" to "accounts/organization",
    // Features.
    ":authentication" to "features/authentication",
    ":onboarding" to "features/onboarding",
    ":account" to "features/account",
    ":location" to "features/location",
    ":socialfeed" to "features/socialfeed",
    ":travel" to "features/travel",
    ":websites" to "features/websites",
)
modules.forEach { (path, dir) ->
    include(path)
    project(path).projectDir = file(dir)
}
