plugins {
    id("com.android.application")
}

android {
    namespace = "com.giftledger.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.giftledger.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
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
}
// 외부 라이브러리 없음: 안드로이드 기본 API만 사용
