package com.example.data.backup

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.util.Base64
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.NewMeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.GeneralSecurityException

internal data class BackupCell(val type: Int, val value: String?)
internal data class BackupTable(val name: String, val columns: List<String>, val rows: List<List<BackupCell>>)
class PreparedBackup internal constructor(
    internal val schemaVersion: Int,
    internal val identityHash: String,
    internal val tables: List<BackupTable>
) {
    val recordCount: Int get() = tables.sumOf { it.rows.size }
}

class BackupStore(private val database: NewMeDatabase, private val cipher: BackupCipher = BackupCipher()) {
    suspend fun export(password: CharArray): ByteArray = withContext(Dispatchers.IO) {
        val plaintext = database.withTransaction {
            val sql = database.openHelper.writableDatabase
            val tables = JSONArray()
            for (name in tableNames(sql)) {
                currentCoroutineContext().ensureActive()
                val columns = columnNames(sql, name)
                val rows = JSONArray()
                sql.query("SELECT * FROM ${quoted(name)} ORDER BY rowid").use { cursor ->
                    while (cursor.moveToNext()) {
                        currentCoroutineContext().ensureActive()
                        val row = JSONArray()
                        for (column in columns.indices) {
                            val type = cursor.getType(column)
                            val value = when (type) {
                                Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                                Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(column).toString()
                                Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(column).also {
                                    require(it.isFinite()) { "A stored numeric value is invalid." }
                                }.toString()
                                Cursor.FIELD_TYPE_STRING -> cursor.getString(column)
                                Cursor.FIELD_TYPE_BLOB -> Base64.encodeToString(cursor.getBlob(column), Base64.NO_WRAP)
                                else -> throw IOException("Unsupported stored value.")
                            }
                            row.put(JSONArray().put(type).put(value))
                        }
                        rows.put(row)
                    }
                }
                tables.put(JSONObject().put("name", name).put("columns", JSONArray(columns)).put("rows", rows))
            }
            JSONObject().put("format", 1).put("schemaVersion", sql.version)
                .put("identityHash", identityHash(sql)).put("tables", tables)
                .toString().toByteArray(Charsets.UTF_8)
        }
        try {
            currentCoroutineContext().ensureActive()
            cipher.encrypt(plaintext, password)
        } finally {
            plaintext.fill(0)
        }
    }

    suspend fun prepare(backup: ByteArray, password: CharArray): PreparedBackup = withContext(Dispatchers.IO) {
        val plaintext = try {
            cipher.decrypt(backup, password)
        } catch (error: GeneralSecurityException) {
            throw IOException("Wrong password or damaged backup. Existing data has not changed.")
        }
        try {
            currentCoroutineContext().ensureActive()
            val json = JSONObject(plaintext.toString(Charsets.UTF_8))
            require(json.getInt("format") == 1) { "Unsupported backup format." }
            val tableArray = json.getJSONArray("tables")
            val tables = List(tableArray.length()) { tableIndex ->
                val table = tableArray.getJSONObject(tableIndex)
                val columns = table.getJSONArray("columns")
                val rowArray = table.getJSONArray("rows")
                BackupTable(table.getString("name"), List(columns.length()) { columns.getString(it) },
                    List(rowArray.length()) { rowIndex ->
                        val row = rowArray.getJSONArray(rowIndex)
                        List(row.length()) { columnIndex ->
                            val cell = row.getJSONArray(columnIndex)
                            require(cell.length() == 2) { "Invalid backup cell." }
                            BackupCell(cell.getInt(0), if (cell.isNull(1)) null else cell.getString(1))
                        }
                    })
            }
            val prepared = PreparedBackup(json.getInt("schemaVersion"), json.getString("identityHash"), tables)
            database.withTransaction { validate(database.openHelper.writableDatabase, prepared) }
            prepared
        } finally {
            plaintext.fill(0)
        }
    }

    suspend fun restore(prepared: PreparedBackup) = withContext(Dispatchers.IO) {
        database.withTransaction {
            val sql = database.openHelper.writableDatabase
            validate(sql, prepared)
            currentCoroutineContext().ensureActive()
            sql.execSQL("PRAGMA defer_foreign_keys = ON")
            prepared.tables.forEach { sql.execSQL("DELETE FROM ${quoted(it.name)}") }
            for (table in prepared.tables) {
                val placeholders = table.columns.joinToString(",") { "?" }
                val columns = table.columns.joinToString(",", transform = ::quoted)
                sql.compileStatement("INSERT INTO ${quoted(table.name)} ($columns) VALUES ($placeholders)").use { statement ->
                    for (row in table.rows) {
                        currentCoroutineContext().ensureActive()
                        statement.clearBindings()
                        row.forEachIndexed { index, cell ->
                            when (cell.type) {
                                Cursor.FIELD_TYPE_NULL -> statement.bindNull(index + 1)
                                Cursor.FIELD_TYPE_INTEGER -> statement.bindLong(index + 1, cell.value!!.toLong())
                                Cursor.FIELD_TYPE_FLOAT -> statement.bindDouble(index + 1, cell.value!!.toDouble())
                                Cursor.FIELD_TYPE_STRING -> statement.bindString(index + 1, cell.value!!)
                                Cursor.FIELD_TYPE_BLOB -> statement.bindBlob(index + 1, Base64.decode(cell.value, Base64.NO_WRAP))
                            }
                        }
                        statement.executeInsert()
                    }
                }
            }
            sql.execSQL("UPDATE app_settings SET cycleRemindersEnabled = 0")
            sql.execSQL("UPDATE activity_tracks SET status = 'PAUSED', lastStepCounter = -1, routeSegment = routeSegment + 1, warning = 'Restored session is paused; review and resume explicitly.' WHERE sessionKey IN (SELECT sessionKey FROM active_tracking)")
            sql.query("PRAGMA foreign_key_check").use { require(!it.moveToFirst()) { "Backup contains broken record relationships." } }
            currentCoroutineContext().ensureActive()
        }
    }

    suspend fun exportTo(resolver: ContentResolver, uri: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        requireLocalDocument(uri)
        val encrypted = export(password)
        try {
            currentCoroutineContext().ensureActive()
            val output = resolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open the local backup destination.")
            output.use { stream ->
                var offset = 0
                while (offset < encrypted.size) {
                    currentCoroutineContext().ensureActive()
                    val length = minOf(64 * 1024, encrypted.size - offset)
                    stream.write(encrypted, offset, length)
                    offset += length
                }
                stream.flush()
            }
        } finally {
            encrypted.fill(0)
        }
    }

    suspend fun prepareFrom(resolver: ContentResolver, uri: Uri, password: CharArray): PreparedBackup = withContext(Dispatchers.IO) {
        requireLocalDocument(uri)
        val input = resolver.openInputStream(uri) ?: throw IOException("Cannot read the local backup.")
        val encrypted = input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(64 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = stream.read(buffer)
                if (count < 0) break
                require(output.size().toLong() + count <= BackupCipher.MAX_BACKUP_BYTES) { "Backup exceeds the supported size." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
        try {
            prepare(encrypted, password)
        } finally {
            encrypted.fill(0)
        }
    }

    private fun validate(sql: SupportSQLiteDatabase, backup: PreparedBackup) {
        require(backup.schemaVersion == sql.version && backup.identityHash == identityHash(sql)) {
            "This backup uses an incompatible database schema. Existing data has not changed."
        }
        val expected = tableNames(sql)
        require(backup.tables.map { it.name }.sorted() == expected) { "Backup is missing tables or contains unknown tables." }
        for (table in backup.tables) {
            require(table.columns == columnNames(sql, table.name)) { "Backup columns do not match this app version." }
            val primaryKeys = mutableListOf<Int>()
            val required = mutableListOf<Int>()
            sql.query("PRAGMA table_info(${quoted(table.name)})").use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getInt(5) > 0) primaryKeys.add(cursor.getInt(0))
                    if (cursor.getInt(3) > 0) required.add(cursor.getInt(0))
                }
            }
            val seen = HashSet<List<BackupCell>>()
            for (row in table.rows) {
                require(row.size == table.columns.size) { "Invalid backup row." }
                require(required.all { row[it].type != Cursor.FIELD_TYPE_NULL }) { "Backup is missing a required value." }
                for (cell in row) {
                    when (cell.type) {
                        Cursor.FIELD_TYPE_NULL -> require(cell.value == null)
                        Cursor.FIELD_TYPE_INTEGER -> require(cell.value?.toLongOrNull() != null)
                        Cursor.FIELD_TYPE_FLOAT -> require(cell.value?.toDoubleOrNull()?.isFinite() == true)
                        Cursor.FIELD_TYPE_STRING -> require(cell.value != null)
                        Cursor.FIELD_TYPE_BLOB -> { require(cell.value != null); Base64.decode(cell.value, Base64.NO_WRAP) }
                        else -> throw IOException("Unsupported backup value.")
                    }
                }
                require(seen.add(primaryKeys.map { row[it] })) { "Backup contains duplicate record identities." }
            }
        }
    }

    private fun tableNames(sql: SupportSQLiteDatabase): List<String> = buildList {
        sql.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name NOT IN ('room_master_table', 'android_metadata') ORDER BY name").use {
            while (it.moveToNext()) add(it.getString(0))
        }
    }

    private fun columnNames(sql: SupportSQLiteDatabase, table: String): List<String> = buildList {
        sql.query("PRAGMA table_info(${quoted(table)})").use { while (it.moveToNext()) add(it.getString(1)) }
    }

    private fun identityHash(sql: SupportSQLiteDatabase): String = sql.query("SELECT identity_hash FROM room_master_table WHERE id = 42").use {
        require(it.moveToFirst()) { "Database schema could not be verified." }
        it.getString(0)
    }

    private fun quoted(name: String): String = "\"${name.replace("\"", "\"\"")}\""

    companion object {
        fun requireLocalDocument(uri: Uri) {
            require(uri.scheme == "content" && uri.authority in setOf(
                "com.android.externalstorage.documents", "com.android.providers.downloads.documents", "com.android.providers.media.documents"
            )) { "Choose device storage or Downloads. Cloud document providers are not supported." }
        }
    }
}