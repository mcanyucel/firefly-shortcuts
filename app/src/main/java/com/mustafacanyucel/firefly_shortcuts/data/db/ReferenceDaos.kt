package com.mustafacanyucel.firefly_shortcuts.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert


interface ReferenceDao<T> {
    @Upsert
    suspend fun upsertAll(items: List<T>)

    @Delete
    suspend fun deleteAll(items: List<T>)
}

@Dao
interface AccountDao : ReferenceDao<AccountEntity> {
    @Query("SELECT * FROM accounts ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<AccountEntity>

    @Query("SELECT fromAccountId FROM shortcuts UNION SELECT toAccountId FROM shortcuts")
    suspend fun usedIds(): List<String>
}

@Dao
interface BillDao : ReferenceDao<BillEntity> {
    @Query("SELECT * FROM bills ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<BillEntity>
}

@Dao
interface BudgetDao : ReferenceDao<BudgetEntity> {
    @Query("SELECT * FROM budgets ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<BudgetEntity>
}

@Dao
interface CategoryDao : ReferenceDao<CategoryEntity> {
    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<CategoryEntity>
}

@Dao
interface PiggybankDao : ReferenceDao<PiggybankEntity> {
    @Query("SELECT * FROM piggybanks ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<PiggybankEntity>
}

@Dao
interface TagDao : ReferenceDao<TagEntity> {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<TagEntity>
}
