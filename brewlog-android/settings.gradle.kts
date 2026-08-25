pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        // PrintBeam binary distribution (how a real consumer resolves it)
        maven("https://kotimadduluri.github.io/printbeam-sdk/maven")
    }
}

rootProject.name = "brewlog-android"
include(":app")
