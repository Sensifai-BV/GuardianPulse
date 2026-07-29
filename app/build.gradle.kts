import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.guardianpulse.prototype"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.guardianpulse.prototype"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        
        vectorDrawables {
            useSupportLibrary = true
        }

        val properties = Properties()
        val localProperties = project.rootProject.file("local.properties")
        if (localProperties.exists()) {
            properties.load(localProperties.inputStream())
        }
        val botToken = properties.getProperty("TELEGRAM_BOT_TOKEN") ?: ""
        val chatId = properties.getProperty("TELEGRAM_CHAT_ID") ?: ""
        val chatId2 = properties.getProperty("TELEGRAM_CHAT_ID_2") ?: ""
        val chatId3 = properties.getProperty("TELEGRAM_CHAT_ID_3") ?: ""
        
        buildConfigField("String", "TELEGRAM_BOT_TOKEN", "\"$botToken\"")
        buildConfigField("String", "TELEGRAM_CHAT_ID", "\"$chatId\"")
        buildConfigField("String", "TELEGRAM_CHAT_ID_2", "\"$chatId2\"")
        buildConfigField("String", "TELEGRAM_CHAT_ID_3", "\"$chatId3\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
