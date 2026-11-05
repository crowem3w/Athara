











import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "soc.athara.app.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "soc.athara.app"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    
    
    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"") 
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "true")
        }
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release") 
            buildConfigField("String", "API_BASE_URL", "\"https://api.staging.example.org/\"") 
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "false")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api.example.org/\"") 
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

// Splash logo lives in assets/images/app (see assets/README.md).
androidComponents {
    val imagesDir = rootProject.file("assets/images/app").absolutePath
    onVariants { variant ->
        variant.sources.res?.addStaticSourceDirectory(imagesDir)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":network"))
    implementation(project(":sync-engine"))
    implementation(project(":identity-api"))
    implementation(project(":onboarding"))
    implementation(project(":authentication"))
    implementation(project(":personal"))
    implementation(project(":business"))
    implementation(project(":organization"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
}