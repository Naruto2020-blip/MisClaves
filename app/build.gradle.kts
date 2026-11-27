plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.misclaves.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.misclaves.app"
        minSdk = 26
        targetSdk = 35
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("VERSION_NAME") ?: "1.0"
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("ANDROID_KEYSTORE_PATH")
            val storePass = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            val keyAliasValue = System.getenv("ANDROID_KEY_ALIAS")
            val keyPass = System.getenv("ANDROID_KEY_PASSWORD")
            if (!storePath.isNullOrBlank() && !storePass.isNullOrBlank() &&
                !keyAliasValue.isNullOrBlank() && !keyPass.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = storePass
                keyAlias = keyAliasValue
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        getByName("release") {
            val signingReady = listOf(
                "ANDROID_KEYSTORE_PATH", "ANDROID_KEYSTORE_PASSWORD",
                "ANDROID_KEY_ALIAS", "ANDROID_KEY_PASSWORD"
            ).all { !System.getenv(it).isNullOrBlank() }
            if (signingReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
