package online.bingzi.stars.plugin.api

import com.mikuac.shiro.core.BotContainer
import online.bingzi.stars.plugin.api.db.Database
import java.io.File

abstract class JavaPlugin : Plugin {
    private lateinit var _description: PluginDescription
    private lateinit var _dataFolder: File
    private lateinit var _logger: PluginLogger
    private lateinit var _pluginManager: PluginManager
    private lateinit var _botContainer: BotContainer
    private lateinit var _database: Database

    @Volatile
    private var _enabled: Boolean = false

    override val description: PluginDescription get() = _description
    override val dataFolder: File get() = _dataFolder
    override val logger: PluginLogger get() = _logger
    override val pluginManager: PluginManager get() = _pluginManager
    override val botContainer: BotContainer get() = _botContainer
    override val database: Database get() = _database
    override val isEnabled: Boolean get() = _enabled

    internal fun init(
        description: PluginDescription,
        dataFolder: File,
        pluginManager: PluginManager,
        botContainer: BotContainer,
        database: Database,
    ) {
        _description = description
        _dataFolder = dataFolder
        _pluginManager = pluginManager
        _botContainer = botContainer
        _database = database
        _logger = PluginLogger(description.name)
        if (!_dataFolder.exists()) _dataFolder.mkdirs()
    }

    internal fun setEnabled(value: Boolean) {
        _enabled = value
    }
}
