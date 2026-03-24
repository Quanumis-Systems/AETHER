rootProject.name = "aether"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://nexus.inductiveautomation.com/repository/inductiveautomation-releases/") }
        maven { url = uri("https://nexus.inductiveautomation.com/repository/inductiveautomation-thirdparty/") }
        maven { url = uri("https://nexus.inductiveautomation.com/repository/inductiveautomation-snapshots/") }
    }
}

include("core")
include("brain")
include("perspective")
