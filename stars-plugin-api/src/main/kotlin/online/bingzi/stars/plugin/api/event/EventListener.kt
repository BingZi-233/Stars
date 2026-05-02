package online.bingzi.stars.plugin.api.event

fun interface EventListener<E : BotEvent> {
    fun onEvent(e: E): EventResult
}
