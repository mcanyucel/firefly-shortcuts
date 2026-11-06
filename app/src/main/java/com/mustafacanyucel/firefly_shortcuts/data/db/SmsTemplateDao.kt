package com.mustafacanyucel.firefly_shortcuts.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.mustafacanyucel.firefly_shortcuts.sms.SmsTemplate
import kotlinx.coroutines.flow.Flow

/**
 * A template for reading bank SMS. Not Firefly data, so it survives a switch to another server.
 */
@Entity(tableName = "sms_templates")
data class SmsTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Stored as JSON, the same format as an exported template. */
    val template: SmsTemplate,
    /** The message the template was built from. The editor marks the fields on it. */
    val sample: String,
)

@Dao
interface SmsTemplateDao {
    @Query("SELECT * FROM sms_templates ORDER BY id")
    fun observeAll(): Flow<List<SmsTemplateEntity>>

    @Query("SELECT * FROM sms_templates ORDER BY id")
    suspend fun getAll(): List<SmsTemplateEntity>

    @Query("SELECT * FROM sms_templates WHERE id = :id")
    suspend fun getById(id: Long): SmsTemplateEntity?

    /** Inserts when [entity] has id 0, updates otherwise. Returns the id. */
    suspend fun save(entity: SmsTemplateEntity): Long =
        if (entity.id == 0L) insert(entity) else entity.id.also { update(entity) }

    @Insert
    suspend fun insert(entity: SmsTemplateEntity): Long

    @Insert
    suspend fun insertAll(entities: List<SmsTemplateEntity>)

    @Update
    suspend fun update(entity: SmsTemplateEntity)

    @Query("DELETE FROM sms_templates WHERE id = :id")
    suspend fun delete(id: Long)
}
