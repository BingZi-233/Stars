package online.bingzi.stars.plugin.loader

import com.fasterxml.jackson.databind.ObjectMapper
import online.bingzi.stars.plugin.api.PluginDescription
import online.bingzi.stars.plugin.api.PluginException
import java.io.File
import java.util.jar.JarFile

class PluginDescriptionLoader(private val mapper: ObjectMapper) {

    /**
     * Reads and parses the plugin.json descriptor from the root of the given JAR file.
     *
     * @throws PluginException.PluginLoadException if plugin.json is missing in the JAR
     */
    fun load(jar: File): PluginDescription {
        JarFile(jar).use { jf ->
            val entry = jf.getJarEntry("plugin.json")
                ?: throw PluginException.PluginLoadException("plugin.json missing in ${jar.name}")
            jf.getInputStream(entry).use {
                return mapper.readValue(it, PluginDescription::class.java)
            }
        }
    }
}
