plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.zerohomers0.onesteplite"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.zerohomers0.onesteplite"
        minSdk = 34
        targetSdk = 37
        versionCode = 1000248
        versionName = "1.0.4"

        ndk {
            abiFilters += "arm64-v8a"
        }
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

    externalNativeBuild {
        ndkBuild {
            path = file("src/main/cpp/Android.mk")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Provided by the Xposed framework at runtime; never packaged into the APK.
    compileOnly(files("../libs/xposed-api-82.jar"))
}
