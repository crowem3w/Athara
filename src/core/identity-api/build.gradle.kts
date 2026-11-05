





import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "org.example.core.identityapi"
    compileSdk = 37

    defaultConfig {
        minSdk = 30
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}



androidComponents {
    val imagesDir = rootProject.file("assets/images/identity-api").absolutePath
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
    
    api(libs.kotlinx.coroutines.android)
    api(libs.androidx.activity)
    implementation(libs.androidx.core.ktx)
}