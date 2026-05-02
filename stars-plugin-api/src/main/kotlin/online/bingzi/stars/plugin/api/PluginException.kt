package online.bingzi.stars.plugin.api

sealed class PluginException(msg: String, cause: Throwable? = null) : RuntimeException(msg, cause) {
    class PluginLoadException(msg: String, cause: Throwable? = null) : PluginException(msg, cause)
    class PluginDependencyException(msg: String, cause: Throwable? = null) : PluginException(msg, cause)
    class PluginLifecycleException(msg: String, cause: Throwable? = null) : PluginException(msg, cause)
}
