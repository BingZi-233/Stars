package online.bingzi.stars.plugin

import com.mikuac.shiro.core.BotContainer
import online.bingzi.stars.plugin.api.JavaPlugin
import online.bingzi.stars.plugin.api.JavaPluginInternals
import online.bingzi.stars.plugin.api.Plugin
import online.bingzi.stars.plugin.api.PluginException
import online.bingzi.stars.plugin.api.PluginManager
import online.bingzi.stars.plugin.api.event.BotEvent
import online.bingzi.stars.plugin.api.event.EventListener
import online.bingzi.stars.db.DatabaseService
import online.bingzi.stars.plugin.config.StarsPluginsProperties
import online.bingzi.stars.plugin.event.AnnotationScanner
import online.bingzi.stars.plugin.event.EventBus
import online.bingzi.stars.plugin.loader.DependencyResolver
import online.bingzi.stars.plugin.loader.PluginLoader
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener as SpringEventListener
import org.springframework.stereotype.Component
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Core plugin lifecycle manager.
 *
 * Owns the full load → init → onLoad → onEnable → onDisable → unload pipeline.
 * All public methods (except [getPlugin] / [getPlugins] which are read-only) are
 * protected by a [ReentrantLock]; re-entrant calls from within the same thread
 * (e.g. loadPlugin → enablePlugin, unloadPlugin → disablePlugin) work transparently.
 *
 * Downstream collaborators:
 * - [PluginLoader]        — ClassLoader creation & JAR scanning
 * - [DependencyResolver]  — Kahn's topological sort
 * - [PluginRegistry]      — sibling-ClassLoader lookup & cache eviction
 * - [AnnotationScanner]   — @OnXxxMessage → EventBus wiring
 * - [EventBus]            — subscription lifecycle
 * - [JavaPluginInternals] — cross-module bridge to internal JavaPlugin methods
 */
@Component
class PluginManagerImpl(
    val botContainer: BotContainer,
    private val eventBus: EventBus,
    private val scanner: AnnotationScanner,
    private val props: StarsPluginsProperties,
    private val databaseService: DatabaseService,
) : PluginManager {

    private val log = LoggerFactory.getLogger(javaClass)

    private val registry = PluginRegistry()

    /** Lazy so that the plugins directory is created only after Spring context is ready. */
    private val loader by lazy {
        PluginLoader(File(props.dir).also { it.mkdirs() }, registry)
    }

    private fun dataFolderFor(name: String): File =
        File(props.dir, name).also { it.mkdirs() }

    /** Ordered map: insertion order = load/enable order. */
    private val plugins = LinkedHashMap<String, JavaPlugin>()

    /** Preserves the original JAR path for each plugin, needed by reloadPlugin. */
    private val jarPaths = ConcurrentHashMap<String, File>()

    private val lock = ReentrantLock()

    // -------------------------------------------------------------------------
    // Lifecycle listeners
    // -------------------------------------------------------------------------

    /**
     * Discovers and bootstraps all plugins after Spring finishes starting.
     * Order: discover JARs → load all → topological sort → onLoad each → optionally onEnable each.
     */
    @SpringEventListener(ApplicationReadyEvent::class)
    fun bootstrap() = lock.withLock {
        val jars = loader.discoverJars()
        val loaded = mutableMapOf<String, PluginLoader.Loaded>()

        jars.forEach { jar ->
            runCatching { loader.load(jar) }
                .onSuccess { loaded[it.description.name] = it; jarPaths[it.description.name] = jar }
                .onFailure { log.error("bootstrap: load failed for ${jar.name}", it) }
        }

        val descs = loaded.values.map { it.description }
        val ordered = try {
            DependencyResolver.sort(descs)
        } catch (e: PluginException.PluginDependencyException) {
            log.error("bootstrap: dependency resolution failed, aborting plugin startup", e)
            return@withLock
        }

        ordered.forEach { d ->
            val l = loaded[d.name]!!
            JavaPluginInternals.init(
                plugin = l.plugin,
                description = d,
                dataFolder = dataFolderFor(d.name),
                pluginManager = this,
                botContainer = botContainer,
                database = databaseService.forPlugin(d.name),
            )
            val dbInit = runCatching { l.plugin.onDatabaseInit() }
            if (dbInit.isFailure) {
                log.error("bootstrap: ${d.name} onDatabaseInit failed, plugin will be skipped", dbInit.exceptionOrNull())
                return@forEach
            }
            runCatching { l.plugin.onLoad() }
                .onFailure { log.error("bootstrap: ${d.name} onLoad failed", it) }
            plugins[d.name] = l.plugin
        }

        if (props.autoEnable) {
            // 仅启用真正进入 plugins 映射的插件（onDatabaseInit 失败的已被跳过）
            ordered.forEach { d -> if (plugins.containsKey(d.name)) enablePlugin(d.name) }
        }
    }

    /**
     * Gracefully disables and unloads all plugins in reverse load order during shutdown.
     */
    @SpringEventListener(ContextClosedEvent::class)
    fun shutdown() = lock.withLock {
        plugins.keys.toList().reversed().forEach { name ->
            runCatching { unloadPlugin(name, cascade = true) }
                .onFailure { log.warn("shutdown: unload $name failed", it) }
        }
    }

    // -------------------------------------------------------------------------
    // PluginManager API
    // -------------------------------------------------------------------------

    /**
     * Hot-loads a plugin JAR at runtime.
     * Validates hard dependencies are already loaded, calls onLoad, and optionally enables.
     */
    override fun loadPlugin(jar: File): Plugin = lock.withLock {
        val l = loader.load(jar)
        jarPaths[l.description.name] = jar

        // Verify hard dependencies are already present
        l.description.depend.forEach { dep ->
            require(plugins.containsKey(dep)) { "missing dependency: $dep" }
        }

        JavaPluginInternals.init(
            plugin = l.plugin,
            description = l.description,
            dataFolder = dataFolderFor(l.description.name),
            pluginManager = this,
            botContainer = botContainer,
            database = databaseService.forPlugin(l.description.name),
        )
        runCatching { l.plugin.onDatabaseInit() }.onFailure {
            log.error("loadPlugin: ${l.description.name} onDatabaseInit failed", it)
            jarPaths.remove(l.description.name)
            throw PluginException.PluginLoadException("onDatabaseInit failed for ${l.description.name}", it)
        }
        runCatching { l.plugin.onLoad() }
            .onFailure { log.error("loadPlugin: ${l.description.name} onLoad failed", it) }
        plugins[l.description.name] = l.plugin

        if (props.autoEnable) enablePlugin(l.description.name)
        l.plugin
    }

    /**
     * Enables a loaded (but not yet enabled) plugin.
     * Scans annotations → calls onEnable → flips isEnabled flag.
     * On onEnable failure, rolls back subscriptions and returns false.
     */
    override fun enablePlugin(name: String): Boolean = lock.withLock {
        val p = plugins[name] ?: return false
        if (p.isEnabled) return false

        scanner.scanAndRegister(p)
        runCatching { p.onEnable() }.onFailure {
            log.error("enablePlugin: $name onEnable failed", it)
            eventBus.unsubscribeAll(name)
            return false
        }
        JavaPluginInternals.setEnabled(p, true)
        log.info("enablePlugin: $name enabled")
        true
    }

    /**
     * Disables an enabled plugin.
     * Calls onDisable (best-effort) → unsubscribes all events → flips isEnabled flag.
     */
    override fun disablePlugin(name: String): Boolean = lock.withLock {
        val p = plugins[name] ?: return false
        if (!p.isEnabled) return false

        runCatching { p.onDisable() }
            .onFailure { log.warn("disablePlugin: $name onDisable threw", it) }
        eventBus.unsubscribeAll(name)
        JavaPluginInternals.setEnabled(p, false)
        log.info("disablePlugin: $name disabled")
        true
    }

    /**
     * Unloads a plugin.
     *
     * With [cascade]=false: refuses if any loaded plugin hard-depends on this one.
     * With [cascade]=true: recursively unloads dependents first (reverse order).
     *
     * Steps: cascade unload dependents → disable → close ClassLoader → clean up maps.
     */
    override fun unloadPlugin(name: String, cascade: Boolean): Boolean = lock.withLock {
        // Find all plugins that hard-depend on this one (must be unloaded first)
        val dependents = plugins.values.filter { name in it.description.depend && it.name != name }
        if (dependents.isNotEmpty() && !cascade) {
            log.warn("unloadPlugin: cannot unload $name — depended on by ${dependents.map { it.name }}")
            return false
        }

        // Cascade: unload dependents in reverse order before unloading this plugin
        if (cascade) {
            dependents.reversed().forEach { dep ->
                unloadPlugin(dep.name, cascade = true)
            }
        }

        // Disable before unloading
        if (plugins[name]?.isEnabled == true) disablePlugin(name)

        val cl = registry.get(name)
        plugins.remove(name)
        jarPaths.remove(name)
        registry.unregister(name)         // also evicts sibling ClassLoader caches
        runCatching { cl?.close() }
            .onFailure { log.warn("unloadPlugin: closing ClassLoader for $name failed", it) }

        log.info("unloadPlugin: $name unloaded")
        true
    }

    /**
     * Reloads a plugin by unloading and re-loading its JAR from the original path.
     * Fails fast if the JAR path is no longer recorded or if unload fails.
     */
    override fun reloadPlugin(name: String): Boolean = lock.withLock {
        val jar = jarPaths[name] ?: run {
            log.warn("reloadPlugin: no jar path recorded for $name")
            return false
        }
        if (!unloadPlugin(name, cascade = false)) return false
        runCatching { loadPlugin(jar) }.onFailure {
            log.error("reloadPlugin: re-load of $name failed", it)
            return false
        }
        true
    }

    override fun getPlugin(name: String): Plugin? = plugins[name]

    override fun getPlugins(): List<Plugin> = lock.withLock { plugins.values.toList() }

    override fun <E : BotEvent> subscribe(
        owner: Plugin,
        type: Class<E>,
        listener: EventListener<E>,
        priority: Int,
    ) = eventBus.subscribe(EventBus.Sub(owner.name, type, listener, priority))
}
