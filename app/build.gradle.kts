plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("kotlinx-serialization")
}

android {
    namespace = "org.ton.wallet"
    compileSdk = Config.compileSdk

    defaultConfig {
        applicationId = "org.ton.wallet"
        minSdk = Config.minSdk
        targetSdk = Config.targetSdk
        versionCode = Config.versionCode
        versionName = Config.versionName
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("./config/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            storeFile = file("./config/release.keystore")
            storePassword = "android"
            keyAlias = "androidreleasekey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = true
            isDebuggable = false
            isJniDebuggable = false
            isShrinkResources = true
            isCrunchPngs = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    coreLibraryDesugaring(Deps.desugaring)

    implementation(Deps.activity)
    implementation(Deps.appCompat)
    implementation(Deps.biometric)
    implementation(Deps.browser)
    implementation(Deps.camera2)
    implementation(Deps.cameraCore)
    implementation(Deps.cameraLifecycle)
    implementation(Deps.cameraView)
    implementation(Deps.contraintLayout)
    implementation(Deps.core)
    implementation(Deps.recyclerView)
    implementation(Deps.security)

    implementation(Deps.barCodeScanner)
    implementation(Deps.conductor)
    implementation(Deps.coil)
    implementation(Deps.coroutines)
    implementation(Deps.easyPermissions)
    implementation(Deps.kotlinJson)
    implementation(Deps.okHttpBom)
    implementation(Deps.okHttpCore)
    implementation(Deps.okHttpLogging)
    implementation(Deps.okHttpSse)
    // used only for data serialization
    implementation(Deps.tonKotlin)

    implementation(project(Modules.rlottie))
    implementation(project(Modules.security))
    implementation(project(Modules.sqlite))
    implementation(project(Modules.tonapi))
    implementation(project(Modules.tonlib))
    implementation(project(Modules.zxing))
}