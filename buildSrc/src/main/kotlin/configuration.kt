object Config {

    const val compileSdk = 33
    const val minSdk = 23
    const val ndkVersion = "21.4.7075529"
    const val targetSdk = 33
    const val versionCode = 1
    const val versionName = "1.0"

    val cmakeArguments = listOf("-DANDROID_STL=c++_static")
}

object Deps {

    private const val CameraVersion = "1.2.2"

    const val activity = "androidx.activity:activity:1.7.1"
    const val annotation = "androidx.annotation:annotation:1.6.0"
    const val appCompat = "androidx.appcompat:appcompat:1.6.1"
    const val barCodeScanner = "com.google.mlkit:barcode-scanning:17.0.3"
    const val biometric = "androidx.biometric:biometric:1.1.0"
    const val browser = "androidx.browser:browser:1.5.0"
    const val camera2 = "androidx.camera:camera-camera2:${CameraVersion}"
    const val cameraCore = "androidx.camera:camera-core:${CameraVersion}"
    const val cameraLifecycle = "androidx.camera:camera-lifecycle:${CameraVersion}"
    const val cameraView = "androidx.camera:camera-view:${CameraVersion}"
    const val coil = "io.coil-kt:coil:2.3.0"
    const val conductor = "com.bluelinelabs:conductor:3.2.0"
    const val contraintLayout = "androidx.constraintlayout:constraintlayout:2.1.4"
    const val core = "androidx.core:core-ktx:1.10.0"
    const val coroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4"
    const val desugaring = "com.android.tools:desugar_jdk_libs:1.2.2"
    const val easyPermissions = "pub.devrel:easypermissions:3.0.0"
    const val kotlinJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0"
    const val okHttpBom = "com.squareup.okhttp3:okhttp-bom:4.11.0"
    const val okHttpCore = "com.squareup.okhttp3:okhttp"
    const val okHttpLogging = "com.squareup.okhttp3:logging-interceptor"
    const val okHttpSse = "com.squareup.okhttp3:okhttp-sse"
    const val recyclerView = "androidx.recyclerview:recyclerview:1.3.0"
    const val security = "androidx.security:security-crypto:1.0.0"
    const val tonKotlin = "org.ton:ton-kotlin:0.2.15"
}

object Modules {

    const val rlottie = ":lib:rlottie"
    const val security = ":lib:security"
    const val sqlite = ":lib:sqlite"
    const val tonapi = ":lib:tonapi"
    const val tonlib = ":lib:tonlib"
    const val zxing = ":lib:zxing"
}