package online.bingzi.stars.plugin.api

import com.mikuac.shiro.core.BotContainer
import online.bingzi.stars.plugin.api.db.Database
import java.io.File

interface Plugin {
    val description: PluginDescription
    val name: String get() = description.name
    val version: String get() = description.version
    val dataFolder: File
    val logger: PluginLogger
    val pluginManager: PluginManager
    val botContainer: BotContainer
    val database: Database
    val isEnabled: Boolean
    fun onLoad() {}

    /**
     * 数据库初始化钩子。
     *
     * 在 [onLoad] 之前由核心调用一次（位于插件 init 之后），用于建表 / 索引等结构准备工作。
     * 抛异常会导致该插件被跳过，不会进入后续 [onLoad] / [onEnable]。
     */
    fun onDatabaseInit() {}
    fun onEnable() {}
    fun onDisable() {}
}
