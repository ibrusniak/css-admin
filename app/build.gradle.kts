plugins {
    alias(libs.plugins.android.application)
}

android {
    lint {
        disable += "OldTargetApi"
    }

    namespace = "com.iBrusniak.cssAdmin"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.iBrusniak.cssAdmin"
        minSdk = 29
        targetSdk = 36
        versionCode = 8
        versionName = "1.0.4"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
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

@Suppress("UnstableApiUsage")
androidComponents {
    onVariants(selector().all()) { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set(
                "${variant.applicationId.get()}-${variant.name}-v${android.defaultConfig.versionName}.apk"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
}