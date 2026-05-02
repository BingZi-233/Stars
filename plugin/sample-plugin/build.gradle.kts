plugins {
    kotlin("jvm") version "2.2.21"
}

group = "online.bingzi.sample"
version = "1.0.0"

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly("online.bingzi:stars-plugin-api:0.0.1-SNAPSHOT")
    compileOnly("com.mikuac:shiro:2.5.2") { isTransitive = false }
    compileOnly("org.slf4j:slf4j-api:2.0.13")
}

tasks.jar {
    archiveBaseName.set("HelloPlugin")
}

tasks.register<Copy>("deploy") {
    dependsOn(tasks.jar)
    from(tasks.jar.flatMap { it.archiveFile })
    into(rootDir.parentFile.parentFile.resolve("plugins"))
}

tasks.named("build") { finalizedBy("deploy") }
