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
    ],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // shortcuts.askAmount
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

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "app.db").build()
    }
}
