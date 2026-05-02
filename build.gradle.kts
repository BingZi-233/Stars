plugins {
    kotlin("jvm") version "2.2.21" apply false
    kotlin("plugin.spring") version "2.2.21" apply false
    id("org.springframework.boot") version "4.0.6" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    group = "online.bingzi"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

val pluginBuildNames = listOf("sample-plugin", "NicknameGuard", "BanQueryPlugin")

val buildPluginApi = tasks.register("buildPluginApi") {
    group = "build"
    description = "Publish stars-plugin-api to mavenLocal (prerequisite for plugin compilation)."
    dependsOn(":stars-plugin-api:publishToMavenLocal")
}

// Phase 2: core bootJar + plugin deploys, run as a separate Gradle invocation so phase 1 jar exists.
val buildCoreAndPlugins = tasks.register<Exec>("buildCoreAndPlugins") {
    group = "build"
    description = "Build stars-core bootJar and deploy all plugins."
    dependsOn(buildPluginApi)
    val gradlew = if (org.gradle.internal.os.OperatingSystem.current().isWindows) "gradlew.bat" else "./gradlew"
    val pluginTasks = pluginBuildNames.map { ":$it:deploy" }
    commandLine(listOf(gradlew, ":stars-core:bootJar") + pluginTasks)
    workingDir = rootDir
}

tasks.register("buildAll") {
    group = "build"
    description = "Build stars-plugin-api, stars-core bootJar, and deploy all plugins to plugins/."
    dependsOn(buildCoreAndPlugins)
}
