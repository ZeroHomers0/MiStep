import java.security.MessageDigest

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

    // The release key never lives in the repository. Provide it through the
    // environment; without it the release build stays unsigned.
    signingConfigs {
        create("release") {
            val storePath = System.getenv("ONESTEPLITE_KEYSTORE")
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = System.getenv("ONESTEPLITE_STORE_PASSWORD")
                keyAlias = System.getenv("ONESTEPLITE_KEY_ALIAS")
                keyPassword = System.getenv("ONESTEPLITE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (System.getenv("ONESTEPLITE_KEYSTORE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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

// Checks the packaged APK the way the module loader will read it: entry class,
// Xposed assets, licence bundle and the uncompressed ARM64 native library.
val verifyApk by tasks.registering(Exec::class) {
    dependsOn("assembleRelease")
    val apk = layout.buildDirectory.file("outputs/apk/release/app-release-unsigned.apk")
    commandLine(
        "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass",
        "-File", rootProject.file("tools/assert_module_apk.ps1").absolutePath,
        "-Apk", apk.get().asFile.absolutePath,
        "-RequireNative",
    )
}

// Copies the verified APK to dist/ with the checksum manifest used by releases.
val stageRelease by tasks.registering {
    dependsOn(verifyApk)
    val apk = layout.buildDirectory.file("outputs/apk/release/app-release-unsigned.apk")
    val dist = rootProject.layout.projectDirectory.dir("dist")
    val versionName = android.defaultConfig.versionName
    doLast {
        val source = apk.get().asFile
        val target = dist.file("OneStepLite-$versionName-unsigned.apk").asFile
        target.parentFile.mkdirs()
        source.copyTo(target, overwrite = true)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(target.readBytes())
            .joinToString("") { byte -> "%02x".format(byte) }
        dist.file("SHA256SUMS.txt").asFile.writeText("$digest  ${target.name}\n")
        logger.lifecycle("Staged ${target.name} ($digest)")
    }
}
