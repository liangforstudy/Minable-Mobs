pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("dev.kikugie.loom-back-compat") version "0.4.3"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// One Fabric build per Minecraft version. 1.21.2+ (render states) and 1.21.9+ (submit queue)
// get added here once 1.21.1 works.
stonecutter {
    create(rootProject) {
        versions("1.21.1")
        vcsVersion = "1.21.1"
    }
}

rootProject.name = "minemobs"
