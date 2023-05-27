plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "org.ton.lib.tonapi"
    compileSdk = Config.compileSdk
    defaultConfig {
        minSdk = Config.minSdk
    }
}

dependencies {
    // used only for data serialization
    implementation(Deps.tonKotlin)
}