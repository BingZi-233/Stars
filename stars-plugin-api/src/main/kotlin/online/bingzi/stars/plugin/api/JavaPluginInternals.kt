package online.bingzi.stars.plugin.api

import com.mikuac.shiro.core.BotContainer
import online.bingzi.stars.plugin.api.db.Database
import java.io.File

/**
 * Friend-access helper — provides stars-core (a different Gradle module) with
 * the ability to call `internal` methods on [JavaPlugin] without exposing them
 * as `public` API to plugin authors.
 *
 * This object lives inside `stars-plugin-api` (same module as [JavaPlugin]),
 * so Kotlin's `internal` visibility is satisfied.
 *
 * **Do NOT call these methods from plugin code.**
 */
object JavaPluginInternals {

    /**
     * Initialises a freshly-instantiated [JavaPlugin] with its runtime dependencies.
     * Must be called by [PluginManagerImpl] immediately after instantiation and before
     * any lifecycle method ([JavaPlugin.onLoad] / [JavaPlugin.onEnable]).
     */
    fun init(
        plugin: JavaPlugin,
        description: PluginDescription,
        dataFolder: File,
        pluginManager: PluginManager,
        botContainer: BotContainer,
        database: Database,
    ) = plugin.init(description, dataFolder, pluginManager, botContainer, database)

    /**
     * Flips the [JavaPlugin.isEnabled] flag.
     * Call with `true` after [JavaPlugin.onEnable] succeeds,
     * and with `false` after [JavaPlugin.onDisable] completes.
     */
    fun setEnabled(plugin: JavaPlugin, value: Boolean) = plugin.setEnabled(value)
}
