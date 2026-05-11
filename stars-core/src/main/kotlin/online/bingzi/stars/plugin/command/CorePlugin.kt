package online.bingzi.stars.plugin.command

import com.mikuac.shiro.core.Bot
import online.bingzi.stars.plugin.api.PluginManager
import online.bingzi.stars.plugin.api.event.BotEvent
import online.bingzi.stars.plugin.api.event.EventListener
import online.bingzi.stars.plugin.api.event.EventResult
import online.bingzi.stars.plugin.config.StarsPluginsProperties
import online.bingzi.stars.plugin.event.EventBus
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener as SpringEventListener
import org.springframework.stereotype.Component
import java.io.File

/**
 * Stars 内置管理指令插件。
 *
 * 以 owner `"@core"` 直接向 EventBus 注册，priority = [Int.MAX_VALUE] 确保
 * `/plugin` 指令优先于所有外部插件响应。不继承 JavaPlugin，是普通 Spring Bean。
 *
 * 支持：
 * - `/help [插件名或命令关键字]`
 * - `/plugin <list|load|unload|reload|enable|disable|info> ...`
 *
 * `/plugin` 仅 [StarsPluginsProperties.admins] 中的 QQ 号可执行。
 */
@Component
class CorePlugin(
    private val eventBus: EventBus,
    private val pluginManager: PluginManager,
    private val props: StarsPluginsProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 应用就绪后向 EventBus 注册私聊和群聊监听器。
     * 使用 [SpringEventListener] 注解（区别于 Stars 自有的 [EventListener] 函数式接口）。
     */
    @SpringEventListener(ApplicationReadyEvent::class)
    fun register() {
        registerHelpListeners()
        registerPluginListeners()
        log.info("CorePlugin: /help and /plugin command listeners registered (admins=${props.admins})")
    }

    private fun registerHelpListeners() {
        eventBus.subscribe(
            EventBus.Sub(
                owner = "@core",
                type = BotEvent.PrivateMessage::class.java,
                listener = EventListener { e ->
                    handleHelp(e.bot, e.event.userId, e.event.rawMessage, group = null)
                },
                priority = Int.MAX_VALUE,
                cmd = "/help",
                usage = "/help [插件名或命令关键字]",
                description = "查询已启用插件命令及用法",
            )
        )
        eventBus.subscribe(
            EventBus.Sub(
                owner = "@core",
                type = BotEvent.GroupMessage::class.java,
                listener = EventListener { e ->
                    handleHelp(e.bot, e.event.userId, e.event.rawMessage, group = e.event.groupId)
                },
                priority = Int.MAX_VALUE,
                cmd = "/help",
                usage = "/help [插件名或命令关键字]",
                description = "查询已启用插件命令及用法",
            )
        )
    }

    private fun registerPluginListeners() {
        eventBus.subscribe(
            EventBus.Sub(
                owner = "@core",
                type = BotEvent.PrivateMessage::class.java,
                listener = EventListener { e ->
                    handlePlugin(e.bot, e.event.userId, e.event.rawMessage, group = null)
                },
                priority = Int.MAX_VALUE,
                cmd = "/plugin",
                usage = "/plugin <list|load|unload|reload|enable|disable|info> [...]",
                description = "插件管理指令，仅管理员可用",
            )
        )
        eventBus.subscribe(
            EventBus.Sub(
                owner = "@core",
                type = BotEvent.GroupMessage::class.java,
                listener = EventListener { e ->
                    handlePlugin(e.bot, e.event.userId, e.event.rawMessage, group = e.event.groupId)
                },
                priority = Int.MAX_VALUE,
                cmd = "/plugin",
                usage = "/plugin <list|load|unload|reload|enable|disable|info> [...]",
                description = "插件管理指令，仅管理员可用",
            )
        )
    }

    // ─── 消息路由 ─────────────────────────────────────────────────────────────

    private fun handleHelp(bot: Bot, userId: Long, raw: String, group: Long?): EventResult {
        val args = raw.trim().split(Regex("\\s+"))
        reply(bot, userId, group, HelpRenderer.render(eventBus.listMessageCommands(), args.getOrNull(1)))
        return EventResult.INTERCEPT
    }

    private fun handlePlugin(bot: Bot, userId: Long, raw: String, group: Long?): EventResult {

        if (userId !in props.admins) {
            reply(bot, userId, group, "未授权：您没有执行插件管理指令的权限。")
            return EventResult.INTERCEPT
        }

        val args = raw.trim().split(Regex("\\s+"))
        val response = runCatching { dispatch(args) }.getOrElse { t ->
            log.warn("CorePlugin: command '${raw}' threw exception", t)
            "执行失败：${t.message ?: t.javaClass.simpleName}"
        }
        reply(bot, userId, group, response)
        return EventResult.INTERCEPT
    }

    // ─── 指令分发 ─────────────────────────────────────────────────────────────

    private fun dispatch(args: List<String>): String = when (args.getOrNull(1)) {
        "list" -> {
            val plugins = pluginManager.getPlugins()
            if (plugins.isEmpty()) "当前无已加载插件。"
            else plugins.joinToString("\n") { p ->
                val state = if (p.isEnabled) "✓" else "✗"
                "[$state] ${p.name} ${p.version}"
            }
        }

        "load" -> {
            val token = args.getOrNull(2) ?: return "用法：/plugin load <jar文件名或插件名>"
            val dir = File(props.dir)
            val jar = resolveJar(dir, token) ?: return "未找到匹配的 JAR：$token"
            val p = pluginManager.loadPlugin(jar)
            "已加载：${p.name} ${p.version}"
        }

        "unload" -> {
            val name = args.getOrNull(2) ?: return "用法：/plugin unload <插件名>"
            if (pluginManager.unloadPlugin(name, cascade = false)) "已卸载：$name"
            else "卸载失败：$name 不存在或被其他插件依赖。"
        }

        "reload" -> {
            val name = args.getOrNull(2) ?: return "用法：/plugin reload <插件名>"
            if (pluginManager.reloadPlugin(name)) "已重载：$name"
            else "重载失败：$name 不存在。"
        }

        "enable" -> {
            val name = args.getOrNull(2) ?: return "用法：/plugin enable <插件名>"
            if (pluginManager.enablePlugin(name)) "已启用：$name"
            else "启用失败：$name 不存在或已启用。"
        }

        "disable" -> {
            val name = args.getOrNull(2) ?: return "用法：/plugin disable <插件名>"
            if (pluginManager.disablePlugin(name)) "已禁用：$name"
            else "禁用失败：$name 不存在或已禁用。"
        }

        "info" -> {
            val name = args.getOrNull(2) ?: return "用法：/plugin info <插件名>"
            val p = pluginManager.getPlugin(name) ?: return "未找到插件：$name"
            buildString {
                appendLine("名称：${p.name}")
                appendLine("版本：${p.version}")
                appendLine("作者：${p.description.authors.joinToString(", ").ifBlank { "未知" }}")
                appendLine("依赖：${p.description.depend.joinToString(", ").ifBlank { "无" }}")
                appendLine("软依赖：${p.description.softdepend.joinToString(", ").ifBlank { "无" }}")
                appendLine("状态：${if (p.isEnabled) "ENABLED" else "DISABLED"}")
                if (p.description.description.isNotBlank()) {
                    append("说明：${p.description.description}")
                }
            }.trimEnd()
        }

        else -> "Stars 插件管理器\n用法：/plugin <list|load|unload|reload|enable|disable|info> [...]"
    }

    private fun resolveJar(dir: File, token: String): File? {
        val direct = dir.resolve(token)
        if (direct.isFile) return direct
        val withExt = dir.resolve("$token.jar")
        if (withExt.isFile) return withExt
        val jars = dir.listFiles { f -> f.isFile && f.extension == "jar" }?.toList().orEmpty()
        val byPrefix = jars.filter { it.nameWithoutExtension.startsWith("$token-") || it.nameWithoutExtension == token }
        return byPrefix.singleOrNull()
    }

    // ─── 回复辅助 ─────────────────────────────────────────────────────────────

    private fun reply(bot: Bot, userId: Long, group: Long?, msg: String) {
        if (group != null) {
            bot.sendGroupMsg(group, msg, false)
        } else {
            bot.sendPrivateMsg(userId, msg, false)
        }
    }
}
