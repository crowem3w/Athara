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





val modules = mapOf(
    ":app" to "app",
    ":personal" to "personal/registration",
    ":business" to "business/registration",
    ":authentication" to "personal/features/authentication",
    ":account" to "personal/features/account",
    ":location" to "personal/features/location",
    ":socialfeed" to "personal/features/socialfeed",
    ":travel" to "personal/features/travel",
    ":network" to "network",
)
modules.forEach { (path, dir) ->
    include(path)
    project(path).projectDir = file(dir)
}