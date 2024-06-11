pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "GrowthBook"
include(":GrowthBook")
include(":Core")
include(":DefaultNetworkDispatcher")
include(":OkHttpNetworkDispatcher")
