package online.bingzi.stars.plugin.api.db

import java.sql.ResultSet

/**
 * 插件可用的数据库访问门面。
 *
 * 由 stars-core 在插件初始化时注入；插件 ClassLoader 不可见 Spring/Hibernate 类型，
 * 因此本接口的参数与返回值仅使用 JDK 标准类型，避免跨 ClassLoader 触发 NoClassDefFoundError。
 *
 * 数据隔离：所有真实表名应通过 [table] 获取，前缀形如 `plugin_<sanitized-name>_`，
 * 由核心实现统一拼装，跨插件不会冲突。
 */
interface Database {
    /** 返回带前缀的真实表名，如 `table("users")` -> `"plugin_banquery_users"`。 */
    fun table(name: String): String

    /** 执行 DDL/DML，返回受影响行数。 */
    fun exec(sql: String, vararg params: Any?): Int

    /** 查询多行；row 由调用方从 [ResultSet] 自行提取（仅在回调内有效）。 */
    fun <T> query(sql: String, vararg params: Any?, mapper: (ResultSet) -> T): List<T>

    /** 查询单行；空结果返回 null；多于一行抛 [IllegalStateException]。 */
    fun <T> queryOne(sql: String, vararg params: Any?, mapper: (ResultSet) -> T): T?

    /** 在事务中执行 [block]；block 抛异常则回滚。 */
    fun <R> transaction(block: (Database) -> R): R
}
