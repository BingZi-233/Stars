package online.bingzi.stars.plugin

import online.bingzi.stars.plugin.loader.PluginClassLoader
import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry of all loaded plugin ClassLoaders, keyed by plugin name.
 *
 * Thread-safe: backed by ConcurrentHashMap. All mutations are via register/unregister.
 */
class PluginRegistry {

    private val byName = ConcurrentHashMap<String, PluginClassLoader>()

    /** Register a freshly-loaded plugin ClassLoader. */
    fun register(name: String, cl: PluginClassLoader) {
        byName[name] = cl
    }

    /**
     * Unregister a plugin by name and evict its classes from all sibling ClassLoader caches.
     * This prevents sibling loaders from holding stale references to classes whose
     * ClassLoader is about to be closed.
     */
    fun unregister(name: String) {
        val gone = byName.remove(name) ?: return
        byName.values.forEach { it.evictByLoader(gone) }
    }

    fun contains(name: String): Boolean = byName.containsKey(name)

    fun get(name: String): PluginClassLoader? = byName[name]

    /** Returns a snapshot of all currently registered loaders keyed by plugin name. */
    fun all(): Map<String, PluginClassLoader> = byName.toMap()

    /**
     * Searches all sibling ClassLoaders (excluding [except]) for the given class.
     * Used by PluginClassLoader to resolve cross-plugin class references.
     *
     * @return the first matching Class, or null if none found
     */
    fun findSiblingClass(fqcn: String, except: PluginClassLoader): Class<*>? {
        byName.values.forEach { sib ->
            if (sib === except) return@forEach
            try {
                return sib.findClass(fqcn, checkSiblings = false)
            } catch (_: ClassNotFoundException) {
                // try next sibling
            }
        }
        return null
    }
}
