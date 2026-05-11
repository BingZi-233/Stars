package online.bingzi.stars.plugin.event

import online.bingzi.stars.plugin.api.Plugin
import online.bingzi.stars.plugin.api.event.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.lang.reflect.Method

private val log = LoggerFactory.getLogger(AnnotationScanner::class.java)

/**
 * 注解扫描器：扫描插件类上的 @OnXxx 注解并向 EventBus 注册订阅。
 *
 * 新策略：注解只决定**类别归属**（及 cmd/pattern/priority 元数据），
 * 订阅的 type 直接取方法**参数类型**，无需为每种 Notice 子类单独映射。
 *
 * 例：`@OnNotice fun onGroupBan(e: BotEvent.GroupBanNotice)` 订阅 BotEvent.GroupBanNotice，
 * 而非旧的 BotEvent.GroupNotice 泛桶。
 *
 * 校验规则：
 * - 方法必须恰好有 1 个参数
 * - 参数类型必须是 BotEvent 的子类
 * - 参数类型必须兼容注解的 expectedBase（如 @OnPrivateMessage 要求参数为 BotEvent.PrivateMessage 或其子类）
 */
@Component
class AnnotationScanner(private val bus: EventBus) {

    /** 注解元数据，cmd/pattern 仅对消息类注解有意义 */
    private class AnnDesc(
        val cmd: String?,
        val pattern: String?,
        val usage: String?,
        val description: String?,
        val priority: Int,
        /** 参数类型必须是该类或其子类，否则 warn + 跳过 */
        val expectedBase: Class<out BotEvent>,
    )

    /** 扫描插件主类并注册。 */
    fun scanAndRegister(owner: Plugin) = scanAndRegister(owner, owner)

    /**
     * 扫描 target 对象并注册，owner 用于归属（卸载清理）。
     * 适合插件在 onEnable 中注册辅助对象。
     */
    fun scanAndRegister(owner: Plugin, target: Any) {
        target.javaClass.methods.forEach { m ->
            val ann = findAnnotation(m) ?: return@forEach

            if (m.parameterCount != 1) {
                log.warn("AnnotationScanner: ${owner.name}#${m.name} expects 1 parameter, got ${m.parameterCount}, skipping")
                return@forEach
            }

            val paramType = m.parameterTypes[0]

            if (!BotEvent::class.java.isAssignableFrom(paramType)) {
                log.warn(
                    "AnnotationScanner: ${owner.name}#${m.name} parameter ${paramType.simpleName} " +
                        "is not a BotEvent subclass, skipping"
                )
                return@forEach
            }

            if (!ann.expectedBase.isAssignableFrom(paramType)) {
                log.warn(
                    "AnnotationScanner: ${owner.name}#${m.name} parameter ${paramType.simpleName} " +
                        "not compatible with annotation (expected ${ann.expectedBase.simpleName} or subclass), skipping"
                )
                return@forEach
            }

            m.isAccessible = true

            val regexPattern = ann.pattern?.takeIf(String::isNotEmpty)?.let { raw ->
                runCatching { Regex(raw) }.onFailure { t ->
                    log.warn("AnnotationScanner: invalid regex '$raw' in ${owner.name}#${m.name}, ignoring pattern", t)
                }.getOrNull()
            }

            @Suppress("UNCHECKED_CAST")
            val listener = EventListener<BotEvent> { e ->
                runCatching { m.invoke(target, e) }.onFailure { t ->
                    log.warn("AnnotationScanner: listener ${owner.name}#${m.name} threw exception", t)
                }
                EventResult.CONTINUE
            }

            @Suppress("UNCHECKED_CAST")
            bus.subscribe(
                EventBus.Sub(
                    owner = owner.name,
                    type = paramType as Class<BotEvent>,
                    listener = listener,
                    priority = ann.priority,
                    cmd = ann.cmd?.takeIf(String::isNotEmpty),
                    pattern = regexPattern,
                    usage = ann.usage?.takeIf(String::isNotEmpty),
                    description = ann.description?.takeIf(String::isNotEmpty),
                )
            )

            log.debug(
                "AnnotationScanner: registered ${owner.name}#${m.name} → ${paramType.simpleName} " +
                    "(priority=${ann.priority}, cmd=${ann.cmd}, pattern=${ann.pattern})"
            )
        }
    }

    /**
     * 从方法上提取第一个受支持的 @OnXxx 注解，返回其元数据；无注解返回 null。
     */
    private fun findAnnotation(m: Method): AnnDesc? {
        m.getAnnotation(OnPrivateMessage::class.java)?.let {
            return AnnDesc(it.cmd, it.pattern, it.usage, it.description, it.priority, BotEvent.PrivateMessage::class.java)
        }
        m.getAnnotation(OnGroupMessage::class.java)?.let {
            return AnnDesc(it.cmd, it.pattern, it.usage, it.description, it.priority, BotEvent.GroupMessage::class.java)
        }
        m.getAnnotation(OnGuildMessage::class.java)?.let {
            return AnnDesc(it.cmd, it.pattern, it.usage, it.description, it.priority, BotEvent.GuildMessage::class.java)
        }
        m.getAnnotation(OnAnyMessage::class.java)?.let {
            return AnnDesc(it.cmd, it.pattern, it.usage, it.description, it.priority, BotEvent::class.java)
        }
        m.getAnnotation(OnNotice::class.java)?.let {
            return AnnDesc(null, null, null, null, it.priority, BotEvent::class.java)
        }
        m.getAnnotation(OnRequest::class.java)?.let {
            return AnnDesc(null, null, null, null, it.priority, BotEvent::class.java)
        }
        m.getAnnotation(OnMetaEvent::class.java)?.let {
            return AnnDesc(null, null, null, null, it.priority, BotEvent::class.java)
        }
        return null
    }
}
