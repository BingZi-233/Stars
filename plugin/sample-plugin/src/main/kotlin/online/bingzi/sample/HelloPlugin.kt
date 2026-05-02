package online.bingzi.sample

import online.bingzi.stars.plugin.api.JavaPlugin
import online.bingzi.stars.plugin.api.event.BotEvent
import online.bingzi.stars.plugin.api.event.OnGroupMessage
import online.bingzi.stars.plugin.api.event.OnPrivateMessage

class HelloPlugin : JavaPlugin() {

    private val echoRegex = Regex("^/echo (.+)$")

    @OnPrivateMessage(cmd = "hi")
    fun greet(e: BotEvent.PrivateMessage) {
        e.bot.sendPrivateMsg(e.event.userId, "hello", false)
    }

    @OnPrivateMessage(pattern = "^/echo (.+)$")
    fun echoPrivate(e: BotEvent.PrivateMessage) {
        val arg = echoRegex.find(e.event.rawMessage)?.groupValues?.get(1) ?: return
        e.bot.sendPrivateMsg(e.event.userId, arg, false)
    }

    @OnGroupMessage(pattern = "^/echo (.+)$")
    fun echoGroup(e: BotEvent.GroupMessage) {
        val arg = echoRegex.find(e.event.rawMessage)?.groupValues?.get(1) ?: return
        e.bot.sendGroupMsg(e.event.groupId, arg, false)
    }

    override fun onEnable() {
        logger.info("enabled, ${botContainer.robots.size} bots online")
    }

    override fun onDisable() {
        logger.info("disabled")
    }
}
