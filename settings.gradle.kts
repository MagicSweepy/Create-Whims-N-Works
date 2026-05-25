pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven {
            name = "NeoForged Maven"
            url = uri("https://maven.neoforged.net/releases")
        }
    }
}

plugins {
    // Automatic toolchain provisioning.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

