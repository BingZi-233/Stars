package online.bingzi.stars.plugin.config

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan

/**
 * Auto-configuration entry point for the Stars plugin subsystem.
 *
 * Registered via META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 * so it is picked up by Spring Boot's auto-configuration mechanism without
 * requiring an explicit @Import in the application class.
 */
@AutoConfiguration
@EnableConfigurationProperties(StarsPluginsProperties::class)
@ComponentScan("online.bingzi.stars.plugin")
class PluginAutoConfiguration
