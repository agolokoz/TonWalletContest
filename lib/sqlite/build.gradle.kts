plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.ton.lib.sqlite"
    compileSdk = Config.compileSdk
    defaultConfig {
        minSdk = Config.minSdk
    }
}

dependencies {
    implementation(Deps.coroutines)
}