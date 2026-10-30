







import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "social.ahtara.authentication"
    compileSdk = 37

    defaultConfig {
        minSdk = 30
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Gateway root (incl. /api/v1). Override with -Pahtara.apiBaseUrl=... / -Pahtara.apiBaseUrlRelease=...
    // or in ~/.gradle/gradle.properties. 10.0.2.2 is the host machine as seen from the Android emulator.
    buildTypes {
        fun url(key: String, default: String) = (project.findProperty(key) as String? ?: default).trim()
        debug {
            buildConfigField("String", "API_BASE_URL", "\"${url("ahtara.apiBaseUrl", "http://10.0.2.2:8080/api/v1")}\"")
        }
        release {
            buildConfigField(
                "String", "API_BASE_URL",
                "\"${url("ahtara.apiBaseUrlRelease", "https://api.example.invalid/api/v1")}\"",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.google.googleid)
    debugImplementation(libs.androidx.ui.tooling)
}