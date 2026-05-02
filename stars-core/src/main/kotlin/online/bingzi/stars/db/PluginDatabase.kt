package online.bingzi.stars.db

import online.bingzi.stars.plugin.api.db.Database
import org.springframework.dao.IncorrectResultSizeDataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.transaction.support.TransactionTemplate
import java.sql.ResultSet

/**
 * 单插件视角的 [Database] 实现，所有表名调用 [table] 自动加上 [prefix]。
 *
 * 与所有其他插件共享同一个底层 [JdbcTemplate] / DataSource；隔离仅通过表名前缀实现。
 */
internal class PluginDatabase(
    private val prefix: String,
    private val jdbc: JdbcTemplate,
    private val tx: TransactionTemplate,
) : Database {

    override fun table(name: String): String = "${prefix}_$name"

    override fun exec(sql: String, vararg params: Any?): Int =
        jdbc.update(sql, *params)

    override fun <T> query(sql: String, vararg params: Any?, mapper: (ResultSet) -> T): List<T> =
        jdbc.query(sql, RowMapper<T> { rs, _ -> mapper(rs) }, *params)

    override fun <T> queryOne(sql: String, vararg params: Any?, mapper: (ResultSet) -> T): T? =
        try {
            jdbc.queryForObject(sql, RowMapper<T> { rs, _ -> mapper(rs) }, *params)
        } catch (e: IncorrectResultSizeDataAccessException) {
            if (e.actualSize == 0) null else throw IllegalStateException("queryOne returned ${e.actualSize} rows", e)
        }

    @Suppress("UNCHECKED_CAST")
    override fun <R> transaction(block: (Database) -> R): R =
        tx.execute { block(this) } as R
}
