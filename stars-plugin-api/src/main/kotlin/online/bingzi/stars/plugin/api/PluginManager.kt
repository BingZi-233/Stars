package online.bingzi.stars.plugin.api

import online.bingzi.stars.plugin.api.event.BotEvent
import online.bingzi.stars.plugin.api.event.EventListener
import online.bingzi.stars.plugin.api.event.EventResult
import java.io.File

interface PluginManager {
    fun getPlugin(name: String): Plugin?
    fun getPlugins(): List<Plugin>
    fun loadPlugin(jar: File): Plugin
    fun enablePlugin(name: String): Boolean
    fun disablePlugin(name: String): Boolean
    fun unloadPlugin(name: String, cascade: Boolean = false): Boolean
    fun reloadPlugin(name: String): Boolean
    fun <E : BotEvent> subscribe(
        owner: Plugin,
        type: Class<E>,
        listener: EventListener<E>,
        priority: Int = 0,
    )
}

inline fun <reified E : BotEvent> PluginManager.subscribe(
    owner: Plugin,
    priority: Int = 0,
    noinline listener: (E) -> EventResult,
) = subscribe(owner, E::class.java, EventListener { listener(it) }, priority)
