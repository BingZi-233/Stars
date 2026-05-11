package online.bingzi.stars.plugin.event

import online.bingzi.stars.plugin.api.event.BotEvent
import online.bingzi.stars.plugin.api.event.EventListener
import online.bingzi.stars.plugin.api.event.EventResult
import kotlin.test.Test
import kotlin.test.assertEquals

class EventBusTest {

    @Test
    fun `listMessageCommands aggregates scopes and usage metadata`() {
        val bus = EventBus()
        val privateNoop = EventListener<BotEvent.PrivateMessage> { EventResult.CONTINUE }
        val groupNoop = EventListener<BotEvent.GroupMessage> { EventResult.CONTINUE }

        bus.subscribe(
            EventBus.Sub(
                owner = "DemoPlugin",
                type = BotEvent.PrivateMessage::class.java,
                listener = privateNoop,
                cmd = "/echo",
                usage = "/echo <文本>",
                description = "回显输入文本",
            )
        )
        bus.subscribe(
            EventBus.Sub(
                owner = "DemoPlugin",
                type = BotEvent.GroupMessage::class.java,
                listener = groupNoop,
                pattern = Regex("^/echo (.+)$"),
                usage = "/echo <文本>",
                description = "回显输入文本",
            )
        )

        val commands = bus.listMessageCommands()

        assertEquals(1, commands.size)
        assertEquals("DemoPlugin", commands.single().owner)
        assertEquals("/echo <文本>", commands.single().usage)
        assertEquals(
            setOf(EventBus.MessageScope.PRIVATE, EventBus.MessageScope.GROUP),
            commands.single().scopes,
        )
    }

    @Test
    fun `listMessageCommands reflects unsubscribe`() {
        val bus = EventBus()
        val groupNoop = EventListener<BotEvent.GroupMessage> { EventResult.CONTINUE }

        bus.subscribe(
            EventBus.Sub(
                owner = "DemoPlugin",
                type = BotEvent.GroupMessage::class.java,
                listener = groupNoop,
                cmd = "/demo",
                usage = "/demo",
            )
        )
        bus.unsubscribeAll("DemoPlugin")

        assertEquals(emptyList(), bus.listMessageCommands())
    }
}
