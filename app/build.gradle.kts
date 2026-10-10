plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.wxglass"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.wxglass"
        minSdk = 26
        targetSdk = 34
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
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
    // 弹簧动画（Telegram 同款物理手感）。会被打进模块 dex，宿主无需提供
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
}
