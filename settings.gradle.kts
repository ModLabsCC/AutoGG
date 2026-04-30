pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://nexus.modlabs.cc/repository/maven-mirrors/") { name = "ModLabs" }
    }
    plugins {
        val loom_version: String by settings
        id("net.fabricmc.fabric-loom").version(loom_version)
        val kotlin_version: String by System.getProperties()
        kotlin("jvm").version(kotlin_version)
    }
}