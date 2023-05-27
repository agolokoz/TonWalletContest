plugins {
    id("com.android.library")
}

android {
    namespace = "org.ton.lib.tonlib"
    compileSdk = Config.compileSdk
    defaultConfig {
        minSdk = Config.minSdk
    }
    sourceSets.getByName("main"){
        jniLibs.srcDir("./libs")
    }
}