plugins {
    id("com.android.library")
}

android {
    namespace = "org.ton.lib.security"
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
    implementation(Deps.annotation)
}