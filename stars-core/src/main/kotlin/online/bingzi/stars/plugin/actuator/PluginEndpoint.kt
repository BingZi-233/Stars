package online.bingzi.stars.plugin.actuator

import online.bingzi.stars.plugin.api.PluginManager
import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.boot.actuate.endpoint.annotation.Selector
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation
import org.springframework.stereotype.Component

/**
 * Stars 插件系统 Spring Boot Actuator 端点。
 *
 * 暴露在 `/actuator/plugins`，需在 `application.yaml` 中开启：
 * ```yaml
 * management.endpoints.web.exposure.include: health,info,plugins
 * ```
 *
 * ## 操作示例
 * ```
 * # 列出所有插件
 * GET  /actuator/plugins
 *
 * # 查看单个插件
 * GET  /actuator/plugins/HelloPlugin
 *
 * # 执行操作（enable / disable / reload / unload）
 * POST /actuator/plugins/HelloPlugin
 * Content-Type: application/json
 * {"action": "reload"}
 * ```
 */
@Component
@Endpoint(id = "plugins")
class PluginEndpoint(private val pm: PluginManager) {

    /**
     * 插件信息视图（只读 DTO，避免暴露内部类型）。
     */
    data class PluginInfo(
        val name: String,
        val version: String,
        val enabled: Boolean,
        val authors: List<String>,
        val depend: List<String>,
        val softdepend: List<String>,
        val description: String,
    )

    /**
     * 列出全部插件。
     * `GET /actuator/plugins`
     */
    @ReadOperation
    fun list(): List<PluginInfo> = pm.getPlugins().map(::toInfo)

    /**
     * 查询单个插件详情。
     * `GET /actuator/plugins/{name}`
     */
    @ReadOperation
    fun show(@Selector name: String): PluginInfo? = pm.getPlugin(name)?.let(::toInfo)

    /**
     * 对指定插件执行操作。
     * `POST /actuator/plugins/{name}` — body: `{"action": "enable"|"disable"|"reload"|"unload"}`
     */
    @WriteOperation
    fun action(@Selector name: String, action: String): Map<String, Any> {
        val ok = when (action) {
            "enable"  -> pm.enablePlugin(name)
            "disable" -> pm.disablePlugin(name)
            "reload"  -> pm.reloadPlugin(name)
            "unload"  -> pm.unloadPlugin(name, cascade = false)
            else      -> return mapOf("ok" to false, "error" to "未知操作：$action（可用：enable/disable/reload/unload）")
        }
        return mapOf("ok" to ok, "name" to name, "action" to action)
    }

    // ─── 内部辅助 ─────────────────────────────────────────────────────────────

    private fun toInfo(p: online.bingzi.stars.plugin.api.Plugin) = PluginInfo(
        name        = p.name,
        version     = p.version,
        enabled     = p.isEnabled,
        authors     = p.description.authors,
        depend      = p.description.depend,
        softdepend  = p.description.softdepend,
        description = p.description.description,
    )
}
