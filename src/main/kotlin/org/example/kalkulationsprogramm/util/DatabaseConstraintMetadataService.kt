package org.example.kalkulationsprogramm.util

import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet
import java.util.Locale
import java.util.Optional
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Laedt Beschreibungen von Tabellen, Spalten und Constraints aus der Datenbank und stellt
 * sie gecacht zur Verfuegung, damit Fehlerausgaben sprechende Labels verwenden koennen.
 */
@Service
class DatabaseConstraintMetadataService(
    private val jdbcTemplate: JdbcTemplate,
) {
    private val initialised = AtomicBoolean(false)
    private val lock = ReentrantReadWriteLock()

    @Volatile
    private var schemaNameValue = ""

    @Volatile
    private var constraintsByName: Map<String, ConstraintMetadata> = emptyMap()

    @Volatile
    private var columnsByKey: Map<String, ColumnMetadata> = emptyMap()

    @Volatile
    private var columnsByName: Map<String, List<ColumnMetadata>> = emptyMap()

    @Volatile
    private var tablesByName: Map<String, TableMetadata> = emptyMap()

    fun findConstraint(rawName: String?): Optional<ConstraintMetadata> {
        if (rawName.isNullOrBlank()) {
            return Optional.empty()
        }
        ensureLoaded()
        val normalised = normaliseConstraintKey(rawName)
        constraintsByName[normalised]?.let { return Optional.of(it) }
        val dotIndex = normalised.lastIndexOf('.')
        if (dotIndex > -1) {
            constraintsByName[normalised.substring(dotIndex + 1)]?.let { return Optional.of(it) }
        }
        return Optional.empty()
    }

    fun findColumn(tableName: String?, columnName: String?): Optional<ColumnMetadata> {
        if (tableName.isNullOrBlank() || columnName.isNullOrBlank()) {
            return Optional.empty()
        }
        ensureLoaded()
        return Optional.ofNullable(columnsByKey[columnKey(tableName, columnName)])
    }

    fun findColumnByName(columnName: String?): Optional<ColumnMetadata> {
        if (columnName.isNullOrBlank()) {
            return Optional.empty()
        }
        ensureLoaded()
        val matches = columnsByName[normalise(columnName)]
        if (matches.isNullOrEmpty()) {
            return Optional.empty()
        }
        return Optional.of(matches.first())
    }

    fun findTable(tableName: String?): Optional<TableMetadata> {
        if (tableName.isNullOrBlank()) {
            return Optional.empty()
        }
        ensureLoaded()
        return Optional.ofNullable(tablesByName[normalise(tableName)])
    }

    fun getSchemaName(): String {
        ensureLoaded()
        return schemaNameValue
    }

    fun refresh() {
        loadMetadata(true)
    }

    private fun ensureLoaded() {
        if (!initialised.get()) {
            loadMetadata(false)
        }
    }

    private fun loadMetadata(force: Boolean) {
        if (!force && initialised.get()) {
            return
        }
        val writeLock = lock.writeLock()
        writeLock.lock()
        try {
            if (!force && initialised.get()) {
                return
            }

            var schema = ""
            try {
                schema = jdbcTemplate.queryForObject(SQL_SELECT_SCHEMA, String::class.java).orEmpty()
            } catch (ex: DataAccessException) {
                LOG.warn("Could not determine active database schema", ex)
            }
            val finalSchema = schema

            val tableBuffer = mutableMapOf<String, TableMetadata>()
            val columnBuffer = mutableMapOf<String, ColumnMetadata>()
            val columnByNameBuffer = mutableMapOf<String, MutableList<ColumnMetadata>>()
            val builders = linkedMapOf<String, ConstraintMetadataBuilder>()

            try {
                jdbcTemplate.query(SQL_SELECT_TABLES, { ps -> ps.setString(1, finalSchema) }) { rs ->
                    val meta = TableMetadata(
                        rs.getString("table_name"),
                        toDisplayName(rs.getString("table_comment")),
                    )
                    tableBuffer[normalise(meta.tableName)] = meta
                }

                jdbcTemplate.query(SQL_SELECT_COLUMNS, { ps -> ps.setString(1, finalSchema) }) { rs ->
                    val rawLen = rs.getObject("character_maximum_length")
                    val charMaxLen = if (rawLen != null) {
                        val longVal = (rawLen as Number).toLong()
                        if (longVal <= Int.MAX_VALUE) longVal.toInt() else Int.MAX_VALUE
                    } else {
                        null
                    }
                    val meta = ColumnMetadata(
                        rs.getString("table_name"),
                        rs.getString("column_name"),
                        toDisplayName(rs.getString("column_comment")),
                        charMaxLen,
                        !"NO".equals(rs.getString("is_nullable"), ignoreCase = true),
                    )
                    columnBuffer[columnKey(meta.tableName, meta.columnName)] = meta
                    columnByNameBuffer.computeIfAbsent(normalise(meta.columnName)) { mutableListOf() }.add(meta)
                }

                jdbcTemplate.query(SQL_SELECT_CONSTRAINTS, { ps -> ps.setString(1, finalSchema) }) { rs: ResultSet ->
                    while (rs.next()) {
                        val name = rs.getString("constraint_name")
                        val table = rs.getString("table_name")
                        val type = ConstraintMetadata.ConstraintType.fromDatabase(rs.getString("constraint_type"))
                        if (type == ConstraintMetadata.ConstraintType.UNKNOWN) {
                            continue
                        }
                        val builder = builders.computeIfAbsent(normaliseConstraintKey(name)) {
                            ConstraintMetadataBuilder(name, table, type)
                        }

                        rs.getString("column_name")?.let(builder.columns::add)

                        if (type == ConstraintMetadata.ConstraintType.FOREIGN_KEY) {
                            builder.referencedTable = rs.getString("referenced_table_name")
                            rs.getString("referenced_column_name")?.let(builder.referencedColumns::add)
                        }
                    }
                    null
                }
            } catch (ex: DataAccessException) {
                LOG.warn("Could not load database metadata", ex)
            }

            schemaNameValue = finalSchema
            constraintsByName = builders.mapValues { it.value.build() }
            columnsByKey = columnBuffer.toMap()
            columnsByName = columnByNameBuffer.mapValues { it.value.toList() }
            tablesByName = tableBuffer.toMap()
            initialised.set(true)
        } finally {
            writeLock.unlock()
        }
    }

    private class ConstraintMetadataBuilder(
        private val name: String,
        private val table: String,
        private val type: ConstraintMetadata.ConstraintType,
    ) {
        val columns = linkedSetOf<String>()
        val referencedColumns = linkedSetOf<String>()
        var referencedTable: String? = null

        fun build(): ConstraintMetadata =
            ConstraintMetadata(
                name,
                table,
                type,
                columns.toList(),
                referencedTable,
                referencedColumns.toList(),
            )
    }

    data class ColumnMetadata(
        val tableName: String,
        val columnName: String,
        val label: String,
        val maxLength: Int?,
        val nullable: Boolean,
    )

    data class TableMetadata(
        val tableName: String,
        val label: String,
    )

    data class ConstraintMetadata(
        val name: String,
        val tableName: String,
        val type: ConstraintType,
        val columnNames: List<String>,
        val referencedTableName: String?,
        val referencedColumnNames: List<String>,
    ) {
        enum class ConstraintType {
            UNIQUE,
            FOREIGN_KEY,
            PRIMARY_KEY,
            CHECK,
            UNKNOWN,
            ;

            fun isUnique(): Boolean = this == UNIQUE

            fun isForeignKey(): Boolean = this == FOREIGN_KEY

            companion object {
                fun fromDatabase(value: String?): ConstraintType {
                    if (value == null) {
                        return UNKNOWN
                    }
                    return when (value.uppercase(Locale.ROOT)) {
                        "UNIQUE" -> UNIQUE
                        "FOREIGN KEY" -> FOREIGN_KEY
                        "PRIMARY KEY" -> PRIMARY_KEY
                        "CHECK" -> CHECK
                        else -> UNKNOWN
                    }
                }
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DatabaseConstraintMetadataService::class.java)

        private const val SQL_SELECT_SCHEMA = "SELECT DATABASE()"
        private const val SQL_SELECT_TABLES =
            "SELECT table_name, IFNULL(NULLIF(table_comment, ''), table_name) AS table_comment " +
                "FROM information_schema.tables WHERE table_schema = ?"
        private const val SQL_SELECT_COLUMNS =
            "SELECT table_name, column_name, IFNULL(NULLIF(column_comment, ''), column_name) AS column_comment, " +
                "character_maximum_length, is_nullable FROM information_schema.columns WHERE table_schema = ?"
        private const val SQL_SELECT_CONSTRAINTS =
            "SELECT tc.constraint_name, tc.table_name, tc.constraint_type, kcu.column_name, " +
                "rc.referenced_table_name, kcu.referenced_column_name " +
                "FROM information_schema.table_constraints tc " +
                "LEFT JOIN information_schema.key_column_usage kcu " +
                "  ON tc.constraint_name = kcu.constraint_name " +
                " AND tc.table_schema = kcu.table_schema " +
                " AND tc.table_name = kcu.table_name " +
                "LEFT JOIN information_schema.referential_constraints rc " +
                "  ON tc.constraint_name = rc.constraint_name " +
                " AND tc.table_schema = rc.constraint_schema " +
                "WHERE tc.table_schema = ?"

        private fun columnKey(tableName: String, columnName: String): String =
            normalise(tableName) + "." + normalise(columnName)

        private fun normalise(value: String?): String =
            value?.trim()?.uppercase(Locale.ROOT).orEmpty()

        private fun normaliseConstraintKey(value: String?): String =
            value?.replace('`', ' ')?.trim()?.replace("\\s+".toRegex(), " ")?.uppercase(Locale.ROOT).orEmpty()

        private fun toDisplayName(raw: String?): String {
            if (raw.isNullOrBlank()) {
                return ""
            }
            val cleaned = raw.replace('_', ' ').trim()
            if (cleaned.isEmpty()) {
                return raw
            }
            val builder = StringBuilder(cleaned.length)
            var capitalizeNext = true
            for (character in cleaned) {
                if (character.isWhitespace()) {
                    builder.append(' ')
                    capitalizeNext = true
                } else if (capitalizeNext) {
                    builder.append(character.titlecaseChar())
                    capitalizeNext = false
                } else {
                    builder.append(character.lowercaseChar())
                }
            }
            return builder.toString()
        }
    }
}
