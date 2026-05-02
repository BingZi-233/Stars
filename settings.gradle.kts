rootProject.name = "Stars"

include("stars-plugin-api", "stars-core")

includeBuild("plugin/sample-plugin")
includeBuild("plugin/NicknameGuard")
includeBuild("plugin/BanQueryPlugin")
