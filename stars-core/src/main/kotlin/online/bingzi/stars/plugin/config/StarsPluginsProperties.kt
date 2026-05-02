package online.bingzi.stars.plugin.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Stars 插件系统配置属性。
 * 对应 application.yaml 中的 `stars.plugins.*`。
 */
@ConfigurationProperties(prefix = "stars.plugins")
data class StarsPluginsProperties(
    /** 插件目录，相对于工作目录。同时存放 JAR 和每个插件的 dataFolder（`<name>/` 子目录） */
    var dir: String = "./plugins",
    /** 启动时是否自动 enable 所有已加载插件 */
    var autoEnable: Boolean = true,
    /** 允许执行 /plugin 管理指令的 OneBot 用户 QQ 号白名单 */
    var admins: List<Long> = emptyList(),
)
