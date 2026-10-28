




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

rootProject.name = "test"
include("app")
include("onboarding")
include("authentication")
include("personal")
include("business")
include("organization")
include("location")
include(":core")
include(":account")
include(":travel")
include(":socialfeed")
include(":websites")