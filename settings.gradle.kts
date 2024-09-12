enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

rootProject.name = "GrowthBook-Kotlin"
include(":GrowthBook")
include(":Core")
include(":NetworkDispatcherKtor")
include(":NetworkDispatcherOkHttp")
