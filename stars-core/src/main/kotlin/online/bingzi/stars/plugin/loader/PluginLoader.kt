package online.bingzi.stars.plugin.loader

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import online.bingzi.stars.plugin.PluginRegistry
import online.bingzi.stars.plugin.api.JavaPlugin
import online.bingzi.stars.plugin.api.PluginDescription
import online.bingzi.stars.plugin.api.PluginException
import java.io.File

class PluginLoader(
    private val pluginsDir: File,
    private val registry: PluginRegistry,
    private val parentClassLoader: ClassLoader = PluginLoader::class.java.classLoader,
) {

    private val descLoader = PluginDescriptionLoader(jacksonObjectMapper())

    data class Loaded(
        val description: PluginDescription,
        val classLoader: PluginClassLoader,
        val plugin: JavaPlugin,
    )

    /**
     * Loads a plugin from the given JAR file.
     *
     * Steps:
     *  1. Parse plugin.json → PluginDescription
     *  2. Ensure the plugin name is not already registered
     *  3. Create a PluginClassLoader for the JAR
     *  4. Instantiate the main class (must be a JavaPlugin subclass, no-arg ctor)
     *  5. Register the ClassLoader in the registry
     *
     * Note: JavaPlugin.init(...), onLoad(), and onEnable() are NOT called here.
     * The downstream PluginManagerImpl is responsible for those lifecycle steps.
     *
     * @throws PluginException.PluginLoadException on any load failure
     */
    fun load(jar: File): Loaded {
        val desc = descLoader.load(jar)
        if (registry.contains(desc.name)) {
            throw PluginException.PluginLoadException("duplicate plugin name: ${desc.name}")
        }
        val cl = PluginClassLoader(jar, desc, parentClassLoader, registry)
        try {
            val mainClass = Class.forName(desc.main, true, cl)
            require(JavaPlugin::class.java.isAssignableFrom(mainClass)) {
                "${desc.main} is not a JavaPlugin"
            }
            @Suppress("UNCHECKED_CAST")
            val instance = mainClass.getDeclaredConstructor().newInstance() as JavaPlugin
            cl.plugin = instance
            registry.register(desc.name, cl)
            return Loaded(desc, cl, instance)
        } catch (t: Throwable) {
            cl.close()
            throw PluginException.PluginLoadException("failed to load ${desc.name}: ${t.message}", t)
        }
    }

    /**
     * Returns all JAR files found in the plugins directory.
     */
    fun discoverJars(): List<File> =
        pluginsDir
            .takeIf { it.isDirectory }
            ?.listFiles { f -> f.isFile && f.extension == "jar" }
            ?.toList()
            ?: emptyList()
}
