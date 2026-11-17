plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.streamyauto"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.streamyauto"
        minSdk = 26
        targetSdk = 35
        // Numéro de build GitHub Actions = numéro de la Release (build-N)
        val runNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionCode = runNumber
        versionName = "1.$runNumber"
    }

    // Clé de signature fixe : indispensable pour installer une mise à jour
    // par-dessus la version précédente. Clé et mot de passe dans les secrets GitHub
    // SIGNING_KEYSTORE_B64 et SIGNING_PASSWORD ; sans eux, signature debug.
    val signingPassword = System.getenv("SIGNING_PASSWORD")
    val keystoreFile = rootProject.file("signing/streamy.jks")
    if (!signingPassword.isNullOrBlank() && keystoreFile.exists()) {
        signingConfigs {
            create("streamy") {
                storeFile = keystoreFile
                storePassword = signingPassword
                keyAlias = "streamy"
                keyPassword = signingPassword
            }
        }
        buildTypes.getByName("debug").signingConfig = signingConfigs.getByName("streamy")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Media3
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // Car App Library
    implementation("androidx.car.app:app:1.4.0")
    implementation("androidx.car.app:app-projected:1.4.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Network
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
