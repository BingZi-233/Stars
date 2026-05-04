rootProject.name = "Stars"

include("stars-plugin-api", "stars-core")

file("plugin")
    .listFiles()
    ?.filter { it.isDirectory && it.resolve("build.gradle.kts").exists() }
    ?.forEach { includeBuild(it.relativeTo(rootDir).path) }
