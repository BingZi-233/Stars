package online.bingzi.stars.plugin.api

data class PluginDescription(
    val name: String,
    val version: String,
    val main: String,
    val authors: List<String> = emptyList(),
    val depend: List<String> = emptyList(),
    val softdepend: List<String> = emptyList(),
    val loadbefore: List<String> = emptyList(),
    val apiVersion: String = "1.0",
    val description: String = "",
)
