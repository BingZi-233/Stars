package online.bingzi.stars.db

import online.bingzi.stars.plugin.api.db.Database
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/**
 * 工厂：为每个插件按名字派发一个 [PluginDatabase]，共用底层 [JdbcTemplate] / DataSource。
 *
 * 表名前缀 `plugin_<sanitized>_`：
 * - 小写化
 * - `[^a-z0-9_]` 替换为 `_`
 * - 限制 sanitized 长度 ≤ 32（前缀总长 ≤ 39，留够 24 字符给业务表名仍在 MySQL 64 字符上限内）
 */
@Component
class DatabaseService(
    private val jdbc: JdbcTemplate,
    txManager: PlatformTransactionManager,
) {
    private val tx = TransactionTemplate(txManager)

    fun forPlugin(name: String): Database {
        val prefix = "plugin_${sanitize(name)}"
        return PluginDatabase(prefix, jdbc, tx)
    }

    private fun sanitize(name: String): String {
        require(name.isNotBlank()) { "plugin name must not be blank" }
        val cleaned = name.lowercase().map {
            if (it in 'a'..'z' || it in '0'..'9' || it == '_') it else '_'
        }.joinToString("")
        check(cleaned.length in 1..32) {
            "sanitized plugin name '$cleaned' length=${cleaned.length} out of 1..32"
        }
        return cleaned
    }
}
