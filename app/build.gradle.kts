plugins {
    id("com.android.application")
}

android {
    namespace = "com.shiftpay.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.shiftpay.app"
        minSdk = 23
        targetSdk = 36
        versionCode = 3
        versionName = "1.2.1"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
}
