package com.mustafacanyucel.firefly_shortcuts.data.db
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mustafacanyucel.firefly_shortcuts.model.TransactionType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val accountType: String,
    val accountRole: String?,
    val currencyCode: String?,
    val currencySymbol: String?,
    val active: Boolean,
)

@Entity(tableName = "bills")
data class BillEntity(@PrimaryKey val id: String, val name: String)

@Entity(tableName = "budgets")
data class BudgetEntity(@PrimaryKey val id: String, val name: String)

@Entity(tableName = "categories")
data class CategoryEntity(@PrimaryKey val id: String, val name: String)

@Entity(tableName = "piggybanks")
data class PiggybankEntity(@PrimaryKey val id: String, val name: String)

@Entity(tableName = "tags")
data class TagEntity(@PrimaryKey val id: String, val name: String)

@Entity(
    tableName = "shortcuts",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromAccountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = BillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = BudgetEntity::class,
            parentColumns = ["id"],
            childColumns = ["budgetId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = PiggybankEntity::class,
            parentColumns = ["id"],
            childColumns = ["piggybankId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("fromAccountId"),
        Index("toAccountId"),
        Index("categoryId"),
        Index("billId"),
        Index("budgetId"),
        Index("piggybankId"),
    ],
)
data class ShortcutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    // Kept as text so the decimal is sent to Firefly exactly as typed. When [askAmount] is
    // true, this is only a suggestion for the amount prompt, and may be empty.
    val amount: String,
    val fromAccountId: String,
    val toAccountId: String,
    val categoryId: String? = null,
    val billId: String? = null,
    val budgetId: String? = null,
    val piggybankId: String? = null,
    val icon: String? = null,
    val description: String? = null,
    // Epoch milliseconds.
    val lastUsed: Long? = null,
    val transactionType: TransactionType,
    // Ask for the amount every time the shortcut runs.
    @ColumnInfo(defaultValue = "0") val askAmount: Boolean = false,
)

@Entity(
    tableName = "shortcut_tags",
    primaryKeys = ["shortcutId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = ShortcutEntity::class,
            parentColumns = ["id"],
            childColumns = ["shortcutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // shortcutId is already covered by the primary key.
    indices = [Index("tagId")],
)

data class ShortcutTagEntity(val shortcutId: Long, val tagId: String)
