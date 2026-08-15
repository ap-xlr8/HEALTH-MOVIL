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
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.samsung.com/hc") }
    }
}

rootProject.name = "HealthOSMobile"
include(":app")
include(":wearable")
project(":wearable").projectDir = file("../WEREABLE")
