plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.ibrusniak.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ibrusniak.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
}