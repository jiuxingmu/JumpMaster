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

rootProject.name = "JumpMaster"
include(":app")
include(":core:designsystem")
include(":core:domain")
include(":core:data")
include(":feature:counter")
include(":feature:history")
include(":feature:profile")
