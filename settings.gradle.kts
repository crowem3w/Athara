




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

rootProject.name = "athara-client"




val modulePaths = mapOf(
    
    "secure-storage" to "src/core/secure-storage",
    "network" to "src/core/network",
    "sync-engine" to "src/core/sync-engine",
    "identity-api" to "src/core/identity-api",
    
    "design-system" to "src/shared/design-system",
    "authentication" to "src/shared/authentication",
    "payments" to "src/shared/payments",
    
    "personal" to "src/modules/personal",
    "business" to "src/modules/business",
    "organization" to "src/modules/organization",
    
    "app" to "src/app/application",
    "onboarding" to "src/app/onboarding",
)

modulePaths.forEach { (name, path) ->
    include(name)
    project(":$name").projectDir = file(path)
}