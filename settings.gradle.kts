pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("android").version(extra["kotlin.version"] as String).apply(false)
        id("com.android.application").version(extra["agp.version"] as String).apply(false)
        id("com.android.library").version(extra["agp.version"] as String).apply(false)
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        jcenter() // Warning: this repository is going to shut down soon
    }
}

rootProject.name = "Download"

include(":app")
include(":download")

enableFeaturePreview("VERSION_CATALOGS")