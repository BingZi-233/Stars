package online.bingzi.stars.plugin.command

import online.bingzi.stars.plugin.event.EventBus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HelpRendererTest {

    @Test
    fun `render lists command usages`() {
        val output = HelpRenderer.render(
            listOf(
                EventBus.MessageCommand(
                    owner = "@core",
                    trigger = "/help",
                    usage = "/help [插件名或命令关键字]",
                    description = "查询已启用插件命令及用法",
                    scopes = setOf(EventBus.MessageScope.PRIVATE, EventBus.MessageScope.GROUP),
                ),
                EventBus.MessageCommand(
                    owner = "BanReasonQueryPlugin",
                    trigger = "^/(?:封禁原因|封禁查询|ban-info)(?:\\s+(用户名|邮箱|username|email))?\\s+(.+)$",
                    usage = "/封禁原因 <用户名或邮箱>",
                    description = "查询封禁状态和违规记录；支持显式指定用户名或邮箱",
                    scopes = setOf(EventBus.MessageScope.GROUP),
                ),
            )
        )

        assertTrue(output.contains("用法：/help [插件名或命令关键字]"))
        assertTrue(output.contains("[@core] 私聊, 群聊 | /help [插件名或命令关键字]"))
        assertTrue(output.contains("[BanReasonQueryPlugin] 群聊 | /封禁原因 <用户名或邮箱>"))
    }

    @Test
    fun `render filters by plugin or keyword`() {
        val commands = listOf(
            EventBus.MessageCommand(
                owner = "@core",
                trigger = "/plugin",
                usage = "/plugin info <插件名>",
                description = "插件管理指令，仅管理员可用",
                scopes = setOf(EventBus.MessageScope.PRIVATE),
            ),
            EventBus.MessageCommand(
                owner = "BanReasonQueryPlugin",
                trigger = "^/(?:封禁原因|封禁查询|ban-info)(?:\\s+(用户名|邮箱|username|email))?\\s+(.+)$",
                usage = "/封禁原因 <用户名或邮箱>",
                description = "查询封禁状态和违规记录；支持显式指定用户名或邮箱",
                scopes = setOf(EventBus.MessageScope.GROUP),
            ),
        )

        val pluginFiltered = HelpRenderer.render(commands, "BanReasonQueryPlugin")
        assertTrue(pluginFiltered.contains("Stars 帮助：BanReasonQueryPlugin"))
        assertTrue(pluginFiltered.contains("/封禁原因 <用户名或邮箱>"))
        assertFalse(pluginFiltered.contains("/plugin info <插件名>"))

        val keywordFiltered = HelpRenderer.render(commands, "封禁原因")
        assertTrue(keywordFiltered.contains("/封禁原因 <用户名或邮箱>"))
        assertFalse(keywordFiltered.contains("/plugin info <插件名>"))
    }
}
