package online.bingzi.stars.plugin.api

import org.slf4j.LoggerFactory

class PluginLogger(private val pluginName: String) {
    private val delegate = LoggerFactory.getLogger("plugin.$pluginName")

    fun info(msg: String) = delegate.info("[{}] {}", pluginName, msg)
    fun warn(msg: String, t: Throwable? = null) = delegate.warn("[$pluginName] $msg", t)
    fun error(msg: String, t: Throwable? = null) = delegate.error("[$pluginName] $msg", t)
    fun debug(msg: String) = delegate.debug("[{}] {}", pluginName, msg)
}
