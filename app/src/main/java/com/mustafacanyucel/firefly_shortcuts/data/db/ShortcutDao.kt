package com.mustafacanyucel.firefly_shortcuts.data.db

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class ShortcutDetail(
    @Embedded val shortcut: ShortcutEntity,
    @Relation(parentColumn = "fromAccountId", entityColumn = "id")
    val fromAccount: AccountEntity,
    @Relation(parentColumn = "toAccountId", entityColumn = "id")
    val toAccount: AccountEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value  = ShortcutTagEntity::class,
            parentColumn = "shortcutId",
            entityColumn = "tagId"
        ),
    )
    val tags: List<TagEntity>
)

@Dao
interface ShortcutDao {
    @Transaction
    @Query("SELECT * FROM shortcuts ORDER BY id")
    fun observeAll(): Flow<List<ShortcutDetail>>

    // One-shot read for the widget and the worker.
    @Transaction
    @Query("SELECT * FROM shortcuts ORDER BY id")
    suspend fun getAll(): List<ShortcutDetail>

    @Transaction
    @Query("SELECT * FROM shortcuts WHERE id = :id")
    suspend fun getById(id: Long): ShortcutDetail?

    @Query("SELECT COUNT(*) FROM shortcuts")
    suspend fun count(): Int

    @Query("DELETE FROM shortcuts WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE shortcuts SET lastUsed = :time WHERE id = :id")
    suspend fun updateLastUsed(id: Long, time: Long)

    /** Inserts when [shortcut] has id 0, updates otherwise. Returns the id. */
    @Transaction
    suspend fun save(shortcut: ShortcutEntity, tagIds: List<String>): Long {
        val id = if (shortcut.id == 0L) {
            insert(shortcut)
        } else {
            update(shortcut)
            shortcut.id
        }
        clearTags(id)
        insertTags(tagIds.map { ShortcutTagEntity(id, it) })
        return id
    }

    @Insert
    suspend fun insert(shortcut: ShortcutEntity): Long

    @Update
    suspend fun update(shortcut: ShortcutEntity)

    @Query("DELETE FROM shortcut_tags WHERE shortcutId = :shortcutId")
    suspend fun clearTags(shortcutId: Long)

    @Insert
    suspend fun insertTags(tags: List<ShortcutTagEntity>)
}
