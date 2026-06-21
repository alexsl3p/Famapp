import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    kotlin("kapt")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.kinly.famapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kinly.famapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 36
        versionName = "4.5"
        vectorDrawables {
            useSupportLibrary = true
        }
        buildConfigField("String", "SUPABASE_URL", "\"${localProperties["supabase_url"] ?: "https://placeholder.supabase.co"}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProperties["supabase_anon_key"] ?: "placeholder-anon-key"}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localProperties["google_web_client_id"] ?: "placeholder-client-id.apps.googleusercontent.com"}\"")
    }

    signingConfigs {
        // Фиксированный debug-ключ из репозитория, чтобы SHA-1 подписи был
        // стабильным при сборке в любом окружении (CI/контейнер/локально) —
        // иначе Google Sign-In ломается из-за незарегистрированного SHA-1.
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)

    // Glance — виджеты на главном экране
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Supabase
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)

    // CameraX + MLKit barcode scanning (v0.3 scanner)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    // Google Sign-In (legacy API — works on all devices)
    implementation(libs.play.services.auth)

    // Credential Manager (kept for potential future use)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // DataStore
    implementation(libs.datastore.preferences)

    debugImplementation(libs.androidx.ui.tooling)
}
