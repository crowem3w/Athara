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

// Gradle paths stay flat (":location"); only the folders are grouped.
// Everything for the Personal account type lives under personal/.
// business/ and organization/ are intentionally empty for now. When you add them, register
// their modules here (e.g. ":business" to "business") and in app/build.gradle.kts.
val modules = mapOf(
    ":app" to "app",
    ":core" to "personal/core",
    ":personal" to "personal/registration",
    ":authentication" to "personal/features/authentication",
    ":onboarding" to "personal/features/onboarding",
    ":account" to "personal/features/account",
    ":location" to "personal/features/location",
    ":socialfeed" to "personal/features/socialfeed",
    ":travel" to "personal/features/travel",
    ":websites" to "personal/features/websites",
)
modules.forEach { (path, dir) ->
    include(path)
    project(path).projectDir = file(dir)
}
