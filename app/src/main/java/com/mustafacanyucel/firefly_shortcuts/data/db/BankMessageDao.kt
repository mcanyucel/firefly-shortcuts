package com.mustafacanyucel.firefly_shortcuts.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.mustafacanyucel.firefly_shortcuts.sms.SmsAction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

enum class BankMessageStatus {
    /** Waiting for the user to review it. */
    PENDING,

    /** Being sent to Firefly. */
    SENDING,

    /** Firefly didn't take it; [BankMessageEntity.error] says why. Can be reviewed again. */
    FAILED,
}

/**
 * A bank SMS a template read, waiting to become a Firefly transaction. Only what the template
 * read is kept, not the message itself. Deleted once Firefly has it.
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
    @ColumnInfo(defaultValue = "PENDING") val status: BankMessageStatus = BankMessageStatus.PENDING,
    val error: String? = null,
)

/** Which Firefly asset account a card belongs to. Firefly data, so it goes on a server switch. */
@Entity(
    tableName = "card_accounts",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountId")],
)
data class CardAccountEntity(
    @PrimaryKey val cardEnding: String,
    val accountId: String,
)

@Dao
interface BankMessageDao {
    @Query("SELECT * FROM bank_messages ORDER BY receivedAt DESC")
    fun observeAll(): Flow<List<BankMessageEntity>>

    @Query("SELECT * FROM bank_messages WHERE id = :id")
    suspend fun getById(id: Long): BankMessageEntity?

    @Insert
    suspend fun insert(message: BankMessageEntity): Long

    @Query("UPDATE bank_messages SET status = :status, error = :error WHERE id = :id")
    suspend fun setStatus(id: Long, status: BankMessageStatus, error: String? = null)

    @Query("DELETE FROM bank_messages WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM bank_messages")
    suspend fun deleteAll()

    @Query("SELECT accountId FROM card_accounts WHERE cardEnding = :cardEnding")
    suspend fun accountForCard(cardEnding: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCardAccount(mapping: CardAccountEntity)
}
