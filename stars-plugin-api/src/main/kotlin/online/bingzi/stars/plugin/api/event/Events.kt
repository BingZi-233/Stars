package online.bingzi.stars.plugin.api.event

import com.mikuac.shiro.core.Bot
import com.mikuac.shiro.dto.event.message.GroupMessageEvent
import com.mikuac.shiro.dto.event.message.GuildMessageEvent
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent
import com.mikuac.shiro.dto.event.meta.HeartbeatMetaEvent
import com.mikuac.shiro.dto.event.meta.LifecycleMetaEvent
import com.mikuac.shiro.dto.event.meta.MetaEvent as ShiroMetaEvent
import com.mikuac.shiro.dto.event.notice.ChannelCreatedNoticeEvent
import com.mikuac.shiro.dto.event.notice.ChannelDestroyedNoticeEvent
import com.mikuac.shiro.dto.event.notice.ChannelUpdatedNoticeEvent
import com.mikuac.shiro.dto.event.notice.FriendAddNoticeEvent
import com.mikuac.shiro.dto.event.notice.GroupAdminNoticeEvent
import com.mikuac.shiro.dto.event.notice.GroupBanNoticeEvent
import com.mikuac.shiro.dto.event.notice.GroupCardChangeNoticeEvent
import com.mikuac.shiro.dto.event.notice.GroupDecreaseNoticeEvent
import com.mikuac.shiro.dto.event.notice.GroupHonorChangeNoticeEvent
import com.mikuac.shiro.dto.event.notice.GroupIncreaseNoticeEvent
import com.mikuac.shiro.dto.event.notice.GroupLuckyKingNoticeEvent
import com.mikuac.shiro.dto.event.notice.GroupMessageReactionNoticeEvent
import com.mikuac.shiro.dto.event.notice.GroupMsgDeleteNoticeEvent
import com.mikuac.shiro.dto.event.notice.GroupUploadNoticeEvent
import com.mikuac.shiro.dto.event.notice.MessageEmojiLikeNoticeEvent
import com.mikuac.shiro.dto.event.notice.MessageReactionsUpdatedNoticeEvent
import com.mikuac.shiro.dto.event.notice.PokeNoticeEvent
import com.mikuac.shiro.dto.event.notice.PrivateMsgDeleteNoticeEvent
import com.mikuac.shiro.dto.event.notice.ReceiveOfflineFilesNoticeEvent
import com.mikuac.shiro.dto.event.request.FriendAddRequestEvent
import com.mikuac.shiro.dto.event.request.GroupAddRequestEvent

sealed class BotEvent {
    abstract val bot: Bot

    // ─── Message ─────────────────────────────────────────────────────────────
    data class PrivateMessage(override val bot: Bot, val event: PrivateMessageEvent) : BotEvent()
    data class GroupMessage(override val bot: Bot, val event: GroupMessageEvent) : BotEvent()
    data class GuildMessage(override val bot: Bot, val event: GuildMessageEvent) : BotEvent()

    // ─── Meta ─────────────────────────────────────────────────────────────────
    data class HeartbeatMeta(override val bot: Bot, val event: HeartbeatMetaEvent) : BotEvent()
    data class LifecycleMeta(override val bot: Bot, val event: LifecycleMetaEvent) : BotEvent()
    /** 通用 MetaEvent 兜底（Shiro 仅 onAnyMetaEvent 有，特例无单独子类时使用） */
    data class MetaEvent(override val bot: Bot, val event: ShiroMetaEvent) : BotEvent()

    // ─── Notice (group) ───────────────────────────────────────────────────────
    data class GroupAdminNotice(override val bot: Bot, val event: GroupAdminNoticeEvent) : BotEvent()
    data class GroupBanNotice(override val bot: Bot, val event: GroupBanNoticeEvent) : BotEvent()
    data class GroupIncreaseNotice(override val bot: Bot, val event: GroupIncreaseNoticeEvent) : BotEvent()
    data class GroupDecreaseNotice(override val bot: Bot, val event: GroupDecreaseNoticeEvent) : BotEvent()
    data class GroupUploadNotice(override val bot: Bot, val event: GroupUploadNoticeEvent) : BotEvent()
    data class GroupMsgDeleteNotice(override val bot: Bot, val event: GroupMsgDeleteNoticeEvent) : BotEvent()
    data class GroupHonorChangeNotice(override val bot: Bot, val event: GroupHonorChangeNoticeEvent) : BotEvent()
    data class GroupCardChangeNotice(override val bot: Bot, val event: GroupCardChangeNoticeEvent) : BotEvent()
    data class GroupLuckyKingNotice(override val bot: Bot, val event: GroupLuckyKingNoticeEvent) : BotEvent()
    data class GroupMessageReactionNotice(override val bot: Bot, val event: GroupMessageReactionNoticeEvent) : BotEvent()
    data class PokeNotice(override val bot: Bot, val event: PokeNoticeEvent) : BotEvent()

    // ─── Notice (friend / private) ────────────────────────────────────────────
    data class FriendAddNotice(override val bot: Bot, val event: FriendAddNoticeEvent) : BotEvent()
    data class PrivateMsgDeleteNotice(override val bot: Bot, val event: PrivateMsgDeleteNoticeEvent) : BotEvent()
    data class ReceiveOfflineFilesNotice(override val bot: Bot, val event: ReceiveOfflineFilesNoticeEvent) : BotEvent()

    // ─── Notice (universal) ───────────────────────────────────────────────────
    data class MessageEmojiLikeNotice(override val bot: Bot, val event: MessageEmojiLikeNoticeEvent) : BotEvent()
    data class MessageReactionsUpdatedNotice(override val bot: Bot, val event: MessageReactionsUpdatedNoticeEvent) : BotEvent()

    // ─── Notice (channel) ────────────────────────────────────────────────────
    data class ChannelCreatedNotice(override val bot: Bot, val event: ChannelCreatedNoticeEvent) : BotEvent()
    data class ChannelDestroyedNotice(override val bot: Bot, val event: ChannelDestroyedNoticeEvent) : BotEvent()
    data class ChannelUpdatedNotice(override val bot: Bot, val event: ChannelUpdatedNoticeEvent) : BotEvent()

    // ─── Request ─────────────────────────────────────────────────────────────
    data class FriendRequest(override val bot: Bot, val event: FriendAddRequestEvent) : BotEvent()
    data class GroupRequest(override val bot: Bot, val event: GroupAddRequestEvent) : BotEvent()
}
