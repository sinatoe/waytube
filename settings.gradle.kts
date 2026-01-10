pluginManagement {
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "WayTube"
include(":app")
