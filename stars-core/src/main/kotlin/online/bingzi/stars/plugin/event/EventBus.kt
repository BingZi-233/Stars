package online.bingzi.stars.plugin.event

import online.bingzi.stars.plugin.api.event.BotEvent
import online.bingzi.stars.plugin.api.event.EventListener
import online.bingzi.stars.plugin.api.event.EventResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

private val log = LoggerFactory.getLogger(EventBus::class.java)

/**
 * Stars 内部事件总线。
 *
 * 设计要点：
 * - subs 以 BotEvent 子类型为 key，每个桶保存已按 priority 降序排列的不可变 List。
 * - 写操作（subscribe / unsubscribeAll）使用 synchronized(subs) 保证原子性；
 *   读操作（dispatch）依赖 ConcurrentHashMap 可见性保证，无需加锁。
 * - dispatch 遍历所有桶，对 key 做 isAssignableFrom 检查，支持以父类型订阅（如订阅 BotEvent 基类可接收所有事件）。
 */
@Component
class EventBus {

    /**
     * 单个订阅记录。
     *
     * @param owner   订阅方名称（通常为插件名，内核使用 "@core"）
     * @param type    订阅的 BotEvent 子类型
     * @param listener 事件处理回调
     * @param priority 优先级，数字越大越先触发
     * @param cmd     可选：仅当 rawMessage 完全等于 cmd 或以 "$cmd " 开头时触发（仅对消息事件有效）
     * @param pattern 可选：仅当 rawMessage 匹配此正则时触发（仅对消息事件有效）
     */
    data class Sub<E : BotEvent>(
        val owner: String,
        val type: Class<E>,
        val listener: EventListener<E>,
        val priority: Int = 0,
        val cmd: String? = null,
        val pattern: Regex? = null,
    )

    // 写时复制：每次修改都替换整个 List（immutable snapshot），dispatch 读到的永远是一致快照。
    private val subs = ConcurrentHashMap<Class<out BotEvent>, List<Sub<*>>>()

    /**
     * 注册一个订阅。注册后桶内列表按 priority 降序重新排列。
     */
    fun <E : BotEvent> subscribe(s: Sub<E>) {
        synchronized(subs) {
            val existing = subs[s.type]?.toMutableList() ?: mutableListOf()
            existing.add(s)
            existing.sortByDescending { it.priority }
            subs[s.type] = existing.toList()
        }
    }

    /**
     * 取消某个 owner 的全部订阅（插件卸载时调用）。
     */
    fun unsubscribeAll(owner: String) {
        synchronized(subs) {
            subs.replaceAll { _, list -> list.filter { it.owner != owner } }
        }
    }

    /**
     * 分发事件。
     *
     * 1. 收集所有 type.isAssignableFrom(e.javaClass) 的桶，合并后按 priority 降序排序。
     * 2. 对消息事件提取 rawMessage，应用 cmd / pattern 过滤。
     * 3. 依次调用 listener；若抛出异常则记录 warn 并继续；若返回 INTERCEPT 则立即停止并返回 INTERCEPT。
     * 4. 全部执行完毕返回 CONTINUE。
     */
    @Suppress("UNCHECKED_CAST")
    fun dispatch(e: BotEvent): EventResult {
        // 收集匹配的订阅，合并后全局按 priority 降序
        val matching: List<Sub<*>> = subs.entries
            .filter { (type, _) -> type.isAssignableFrom(e.javaClass) }
            .flatMap { it.value }
            .sortedByDescending { it.priority }

        // 提取消息内容（仅用于 cmd / pattern 过滤）
        val rawMessage: String? = when (e) {
            is BotEvent.PrivateMessage -> e.event.rawMessage
            is BotEvent.GroupMessage -> e.event.rawMessage
            is BotEvent.GuildMessage -> e.event.rawMessage
            else -> null
        }

        for (sub in matching) {
            // cmd 过滤：非消息事件 rawMessage 为 null，若 cmd 非空则跳过
            if (sub.cmd != null) {
                if (rawMessage == null) continue
                if (rawMessage != sub.cmd && !rawMessage.startsWith("${sub.cmd} ")) continue
            }

            // pattern 过滤：非消息事件 rawMessage 为 null，若 pattern 非空则跳过
            if (sub.pattern != null) {
                if (rawMessage == null) continue
                if (!rawMessage.matches(sub.pattern)) continue
            }

            try {
                val result = (sub as Sub<BotEvent>).listener.onEvent(e)
                if (result == EventResult.INTERCEPT) return EventResult.INTERCEPT
            } catch (t: Throwable) {
                log.warn("EventBus: listener [owner=${sub.owner}] threw exception, continuing chain", t)
            }
        }

        return EventResult.CONTINUE
    }
}
