pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "TonWallet"
include(":app")
include(":lib:rlottie")
include(":lib:security")
include(":lib:sqlite")
include(":lib:tonapi")
include(":lib:tonlib")
include(":lib:zxing")
