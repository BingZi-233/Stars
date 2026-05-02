package online.bingzi.stars.plugin.loader

import online.bingzi.stars.plugin.PluginRegistry
import online.bingzi.stars.plugin.api.JavaPlugin
import online.bingzi.stars.plugin.api.PluginDescription
import java.io.Closeable
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap

class PluginClassLoader(
    val jar: File,
    val description: PluginDescription,
    parent: ClassLoader,
    private val registry: PluginRegistry,
) : URLClassLoader(arrayOf(jar.toURI().toURL()), parent), Closeable {

    @Volatile
    var plugin: JavaPlugin? = null
        internal set

    private val classCache = ConcurrentHashMap<String, Class<*>>()

    override fun findClass(name: String): Class<*> = findClass(name, checkSiblings = true)

    internal fun findClass(name: String, checkSiblings: Boolean): Class<*> {
        classCache[name]?.let { return it }
        return try {
            super.findClass(name).also { classCache[name] = it }
        } catch (e: ClassNotFoundException) {
            if (!checkSiblings) throw e
            registry.findSiblingClass(name, this)?.also { classCache[name] = it }
                ?: throw e
        }
    }

    /**
     * Evict all cached classes that were loaded by the given victim ClassLoader.
     * Called by PluginRegistry.unregister to prevent stale class references
     * from lingering in sibling loaders after a plugin is unloaded.
     */
    internal fun evictByLoader(victim: PluginClassLoader) {
        classCache.entries.removeIf { it.value.classLoader === victim }
    }

    override fun close() {
        classCache.clear()
        super.close()
    }
}
