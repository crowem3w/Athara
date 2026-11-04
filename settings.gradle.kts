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
include("app")
include("secure-storage")
include("network")
include("sync-engine")
include("identity-api")
include("design-system")
include("onboarding")
include("authentication")
include("personal")
include("business")
include("organization")
