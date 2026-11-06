package com.mustafacanyucel.firefly_shortcuts.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        AccountEntity::class,
        BillEntity::class,
        BudgetEntity::class,
        CategoryEntity::class,
        PiggybankEntity::class,
        TagEntity::class,
        ShortcutEntity::class,
        ShortcutTagEntity::class,
        SmsTemplateEntity::class,
        BankMessageEntity::class,
        CardAccountEntity::class,
    ],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // shortcuts.askAmount
        AutoMigration(from = 2, to = 3), // sms_templates
        AutoMigration(from = 3, to = 4), // bank_messages
        AutoMigration(from = 4, to = 5), // bank_messages.status and error, card_accounts
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun billDao(): BillDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryDao(): CategoryDao
    abstract fun piggybankDao(): PiggybankDao
    abstract fun tagDao(): TagDao
    abstract fun shortcutDao(): ShortcutDao
    abstract fun smsTemplateDao(): SmsTemplateDao
    abstract fun bankMessageDao(): BankMessageDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "app.db").build()
    }
}
