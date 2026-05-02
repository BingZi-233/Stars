package online.bingzi.stars.plugin.bridge

// TODO: 下游配置类（PluginAutoConfiguration）需要添加 @EnableScheduling 注解，
//       否则 @Scheduled(fixedDelay = 5_000) 的轮询方法不会执行。

import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.core.BotContainer
import com.mikuac.shiro.core.BotPlugin
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(BotContainerInitializer::class.java)

/**
 * Bot 容器初始化器。
 *
 * 职责：确保 [MasterBotPlugin] 始终位于每个 Bot 的 pluginList 最前端，
 * 从而保证所有 Shiro 事件都能被 Stars EventBus 优先接收。
 *
 * 两种触发时机：
 * 1. [ApplicationReadyEvent]：Spring 完全启动后立即注入（处理启动时已连接的 Bot）。
 * 2. [@Scheduled 轮询][ensureAttached]：每 5 秒检查一次，处理 Shiro 反向 WS 模式下
 *    在运行时动态接入的新 Bot。
 *
 * 注意：需要在配置类上添加 @EnableScheduling（见文件头 TODO）。
 */
@Component
class BotContainerInitializer(
    private val container: BotContainer,
) {

    /**
     * 应用就绪后立即将 MasterBotPlugin 注入所有已连接的 Bot。
     */
    @EventListener(ApplicationReadyEvent::class)
    fun onReady() {
        log.info("BotContainerInitializer: application ready, injecting MasterBotPlugin into ${container.robots.size} bot(s)")
        container.robots.values.forEach(::injectMaster)
    }

    /**
     * 定期轮询，将 MasterBotPlugin 注入后续动态接入的 Bot（反向 WS 模式）。
     * 已注入的 Bot 会被跳过，无重复注入。
     */
    @Scheduled(fixedDelay = 5_000)
    fun ensureAttached() {
        container.robots.values.forEach(::injectMaster)
    }

    /**
     * 将 [MasterBotPlugin] 注入指定 Bot 的 pluginList 最前端。
     * 若已存在则跳过（幂等）。
     *
     * 防御性拷贝：避免直接修改 Shiro 内部共享的 pluginList 引用。
     */
    private fun injectMaster(bot: Bot) {
        val masterClass: Class<out BotPlugin> = MasterBotPlugin::class.java
        val current: List<Class<out BotPlugin>> = bot.pluginList ?: emptyList()
        if (masterClass in current) return

        bot.pluginList = ArrayList<Class<out BotPlugin>>(current.size + 1).apply {
            add(masterClass)
            addAll(current)
        }
        log.debug("BotContainerInitializer: injected MasterBotPlugin into bot [selfId=${bot.selfId}]")
    }
}
