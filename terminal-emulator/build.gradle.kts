plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.termux.emulator"
    compileSdk = 36
    ndkVersion = "29.0.14206865"

    defaultConfig {
        minSdk = 26
        ndk {
            abiFilters += listOf("x86", "x86_64", "armeabi-v7a", "arm64-v8a")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.0")
}
