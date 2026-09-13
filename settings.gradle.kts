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
        // Holds the libraries built from the Bugcord-* forks, fetched by the build workflow.
        maven { url = uri("local-repo") }
        mavenLocal()
    }
}

rootProject.name = "BugcordManager"
include(":app")
