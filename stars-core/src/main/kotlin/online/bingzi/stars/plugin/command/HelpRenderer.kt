package online.bingzi.stars.plugin.command

import online.bingzi.stars.plugin.event.EventBus

internal object HelpRenderer {
    private const val HELP_USAGE = "/help [插件名或命令关键字]"

    fun render(commands: List<EventBus.MessageCommand>, keyword: String? = null): String {
        val trimmedKeyword = keyword?.trim().orEmpty()
        val filtered = if (trimmedKeyword.isBlank()) {
            commands
        } else {
            commands.filter { command ->
                command.owner.equals(trimmedKeyword, ignoreCase = true) ||
                    command.trigger.contains(trimmedKeyword, ignoreCase = true) ||
                    command.usage.contains(trimmedKeyword, ignoreCase = true) ||
                    command.description.contains(trimmedKeyword, ignoreCase = true)
            }
        }

        if (filtered.isEmpty()) {
            return if (trimmedKeyword.isBlank()) {
                "当前没有已注册的插件命令。"
            } else {
                "未找到匹配的插件命令：$trimmedKeyword\n用法：$HELP_USAGE"
            }
        }

        val title = if (trimmedKeyword.isBlank()) "Stars 帮助" else "Stars 帮助：$trimmedKeyword"
        return buildString {
            appendLine(title)
            appendLine("用法：$HELP_USAGE")
            filtered.forEach { command ->
                val scopes = command.scopes.joinToString(", ") { it.label }
                append("[${command.owner}] $scopes | ${command.usage}")
                if (command.description.isNotBlank()) {
                    append(" | ${command.description}")
                }
                appendLine()
            }
        }.trimEnd()
    }
}
