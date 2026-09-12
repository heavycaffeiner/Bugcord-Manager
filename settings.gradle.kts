@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            name = "bugcord"
            url = uri("https://maven.aliucord.com/releases")
        }
    }
}

rootProject.name = "BugcordManager"
include(":app")
