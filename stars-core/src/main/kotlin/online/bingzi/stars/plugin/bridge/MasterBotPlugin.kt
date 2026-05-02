package online.bingzi.stars.plugin.bridge

import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.core.BotPlugin
import com.mikuac.shiro.dto.event.message.GroupMessageEvent
import com.mikuac.shiro.dto.event.message.GuildMessageEvent
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent
import com.mikuac.shiro.dto.event.notice.*
import com.mikuac.shiro.dto.event.request.FriendAddRequestEvent
import com.mikuac.shiro.dto.event.request.GroupAddRequestEvent
import online.bingzi.stars.plugin.api.event.BotEvent
import online.bingzi.stars.plugin.api.event.EventResult
import online.bingzi.stars.plugin.event.EventBus
import org.springframework.stereotype.Component

/**
 * Shiro 侧唯一常驻桩插件。
 *
 * 职责：将 Shiro 回调的各类事件封装为对应的 [BotEvent] 子类并转发给 [EventBus]，
 * 由 EventBus 负责按优先级分发给所有已注册的 Stars 插件监听器。
 *
 * 此类通过 [BotContainerInitializer] 在运行时注入到每个 Bot 的 pluginList 最前端，
 * 保证 Stars 优先处理所有事件。owner 使用保留名 "@core"，不受外部插件的
 * EventBus.unsubscribeAll() 影响。
 *
 * 覆写所有 25 个 BotPlugin 钩子（跳过 onAnyMessage 避免重复触发）。
 * 每个 Shiro 事件映射到精确的 BotEvent 子类，插件可按参数类型精确订阅。
 */
@Component
class MasterBotPlugin(private val bus: EventBus) : BotPlugin() {

    // ─── 消息事件 ────────────────────────────────────────────────────────────

    override fun onPrivateMessage(bot: Bot, event: PrivateMessageEvent): Int =
        bus.dispatch(BotEvent.PrivateMessage(bot, event)).toShiro()

    override fun onGroupMessage(bot: Bot, event: GroupMessageEvent): Int =
        bus.dispatch(BotEvent.GroupMessage(bot, event)).toShiro()

    override fun onGuildMessage(bot: Bot, event: GuildMessageEvent): Int =
        bus.dispatch(BotEvent.GuildMessage(bot, event)).toShiro()

    // ─── 群通知事件 ──────────────────────────────────────────────────────────

    override fun onGroupUploadNotice(bot: Bot, event: GroupUploadNoticeEvent): Int =
        bus.dispatch(BotEvent.GroupUploadNotice(bot, event)).toShiro()

    override fun onGroupAdminNotice(bot: Bot, event: GroupAdminNoticeEvent): Int =
        bus.dispatch(BotEvent.GroupAdminNotice(bot, event)).toShiro()

    override fun onGroupDecreaseNotice(bot: Bot, event: GroupDecreaseNoticeEvent): Int =
        bus.dispatch(BotEvent.GroupDecreaseNotice(bot, event)).toShiro()

    override fun onGroupIncreaseNotice(bot: Bot, event: GroupIncreaseNoticeEvent): Int =
        bus.dispatch(BotEvent.GroupIncreaseNotice(bot, event)).toShiro()

    override fun onGroupBanNotice(bot: Bot, event: GroupBanNoticeEvent): Int =
        bus.dispatch(BotEvent.GroupBanNotice(bot, event)).toShiro()

    override fun onGroupMsgDeleteNotice(bot: Bot, event: GroupMsgDeleteNoticeEvent): Int =
        bus.dispatch(BotEvent.GroupMsgDeleteNotice(bot, event)).toShiro()

    override fun onGroupLuckyKingNotice(bot: Bot, event: GroupLuckyKingNoticeEvent): Int =
        bus.dispatch(BotEvent.GroupLuckyKingNotice(bot, event)).toShiro()

    override fun onGroupHonorChangeNotice(bot: Bot, event: GroupHonorChangeNoticeEvent): Int =
        bus.dispatch(BotEvent.GroupHonorChangeNotice(bot, event)).toShiro()

    override fun onGroupCardChangeNotice(bot: Bot, event: GroupCardChangeNoticeEvent): Int =
        bus.dispatch(BotEvent.GroupCardChangeNotice(bot, event)).toShiro()

    override fun onGroupReactionNotice(bot: Bot, event: GroupMessageReactionNoticeEvent): Int =
        bus.dispatch(BotEvent.GroupMessageReactionNotice(bot, event)).toShiro()

    // ─── 好友 / 私聊通知事件 ─────────────────────────────────────────────────

    override fun onFriendAddNotice(bot: Bot, event: FriendAddNoticeEvent): Int =
        bus.dispatch(BotEvent.FriendAddNotice(bot, event)).toShiro()

    override fun onPrivateMsgDeleteNotice(bot: Bot, event: PrivateMsgDeleteNoticeEvent): Int =
        bus.dispatch(BotEvent.PrivateMsgDeleteNotice(bot, event)).toShiro()

    // ─── Poke 通知（群 + 私聊共用同一 BotEvent 类型）────────────────────────

    override fun onGroupPokeNotice(bot: Bot, event: PokeNoticeEvent): Int =
        bus.dispatch(BotEvent.PokeNotice(bot, event)).toShiro()

    override fun onPrivatePokeNotice(bot: Bot, event: PokeNoticeEvent): Int =
        bus.dispatch(BotEvent.PokeNotice(bot, event)).toShiro()

    // ─── 通用通知事件 ────────────────────────────────────────────────────────

    override fun onReceiveOfflineFilesNotice(bot: Bot, event: ReceiveOfflineFilesNoticeEvent): Int =
        bus.dispatch(BotEvent.ReceiveOfflineFilesNotice(bot, event)).toShiro()

    override fun onMessageEmojiLikeNotice(bot: Bot, event: MessageEmojiLikeNoticeEvent): Int =
        bus.dispatch(BotEvent.MessageEmojiLikeNotice(bot, event)).toShiro()

    override fun onMessageReactionsUpdatedNotice(bot: Bot, event: MessageReactionsUpdatedNoticeEvent): Int =
        bus.dispatch(BotEvent.MessageReactionsUpdatedNotice(bot, event)).toShiro()

    // ─── 频道通知事件 ────────────────────────────────────────────────────────

    override fun onChannelCreatedNotice(bot: Bot, event: ChannelCreatedNoticeEvent): Int =
        bus.dispatch(BotEvent.ChannelCreatedNotice(bot, event)).toShiro()

    override fun onChannelDestroyedNotice(bot: Bot, event: ChannelDestroyedNoticeEvent): Int =
        bus.dispatch(BotEvent.ChannelDestroyedNotice(bot, event)).toShiro()

    override fun onChannelUpdatedNotice(bot: Bot, event: ChannelUpdatedNoticeEvent): Int =
        bus.dispatch(BotEvent.ChannelUpdatedNotice(bot, event)).toShiro()

    // ─── 请求事件 ────────────────────────────────────────────────────────────

    override fun onFriendAddRequest(bot: Bot, event: FriendAddRequestEvent): Int =
        bus.dispatch(BotEvent.FriendRequest(bot, event)).toShiro()

    override fun onGroupAddRequest(bot: Bot, event: GroupAddRequestEvent): Int =
        bus.dispatch(BotEvent.GroupRequest(bot, event)).toShiro()

    // ─── 辅助扩展 ────────────────────────────────────────────────────────────

    /**
     * INTERCEPT → MESSAGE_BLOCK（阻止后续 Shiro 插件处理），CONTINUE → MESSAGE_IGNORE。
     */
    private fun EventResult.toShiro(): Int =
        if (this == EventResult.INTERCEPT) MESSAGE_BLOCK else MESSAGE_IGNORE
}
