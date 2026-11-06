package com.mustafacanyucel.firefly_shortcuts.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.mustafacanyucel.firefly_shortcuts.sms.SmsAction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

/**
 * A bank SMS a template read, waiting to become a Firefly transaction. Only what the template
 * read is kept, not the message itself.
 */
@Entity(tableName = "bank_messages")
data class BankMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Epoch milliseconds. */
    val receivedAt: Long,
    val sender: String,
    val templateName: String,
    val action: SmsAction,
    val amount: String?,
    val currency: String?,
    val date: LocalDate?,
    val time: LocalTime?,
    val merchant: String?,
    val cardEnding: String?,
)

@Dao
interface BankMessageDao {
    @Query("SELECT * FROM bank_messages ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<BankMessageEntity>>

    @Insert
    suspend fun insert(message: BankMessageEntity): Long

    @Query("DELETE FROM bank_messages")
    suspend fun deleteAll()
}
