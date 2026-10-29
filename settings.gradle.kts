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
// Everything for the Personal account type lives under personal/, Business under business/.
// organization/ is intentionally empty for now: when you add it, register its module here
// (":organization" to "organization/registration") and in app/build.gradle.kts.
val modules = mapOf(
    ":app" to "app",
    ":personal" to "personal/registration",
    ":business" to "business/registration",
    ":authentication" to "personal/features/authentication",
    ":account" to "personal/features/account",
    ":location" to "personal/features/location",
    ":socialfeed" to "personal/features/socialfeed",
    ":travel" to "personal/features/travel",
)
modules.forEach { (path, dir) ->
    include(path)
    project(path).projectDir = file(dir)
}
