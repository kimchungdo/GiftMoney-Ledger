plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.giftledger.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.giftledger.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 5
        versionName = "0.2.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// 외부 라이브러리 없음: 안드로이드 기본 API와 Kotlin 표준 라이브러리만 사용

