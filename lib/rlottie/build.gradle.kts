plugins {
    id("com.android.library")
}

android {
    namespace = "org.ton.lib.rlottie"
    compileSdk = Config.compileSdk
    ndkVersion = Config.ndkVersion

    defaultConfig {
        minSdk = Config.minSdk
        externalNativeBuild {
            cmake {
                arguments.addAll(Config.cmakeArguments)
            }
        }
    }

    externalNativeBuild {
        cmake {
            path(file("src/main/cpp/CMakeLists.txt"))
        }
    }
}

dependencies {
}