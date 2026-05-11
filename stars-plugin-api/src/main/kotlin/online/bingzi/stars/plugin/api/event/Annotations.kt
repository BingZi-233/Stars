package online.bingzi.stars.plugin.api.event

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnPrivateMessage(
    val cmd: String = "",
    val pattern: String = "",
    val usage: String = "",
    val description: String = "",
    val priority: Int = 0,
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnGroupMessage(
    val cmd: String = "",
    val pattern: String = "",
    val usage: String = "",
    val description: String = "",
    val priority: Int = 0,
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnGuildMessage(
    val cmd: String = "",
    val pattern: String = "",
    val usage: String = "",
    val description: String = "",
    val priority: Int = 0,
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnAnyMessage(
    val cmd: String = "",
    val pattern: String = "",
    val usage: String = "",
    val description: String = "",
    val priority: Int = 0,
)

/**
 * 订阅 Notice 事件。方法参数声明具体类型（如 `BotEvent.GroupBanNotice`）即过滤特定子类；
 * 声明 `BotEvent` 基类则接收所有 Notice。注解不再决定订阅类型，参数类型决定。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnNotice(val priority: Int = 0)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnRequest(val priority: Int = 0)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnMetaEvent(val priority: Int = 0)
