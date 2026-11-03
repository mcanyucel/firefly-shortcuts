package com.mustafacanyucel.firefly_shortcuts.data.db

import androidx.room.TypeConverter
import com.mustafacanyucel.firefly_shortcuts.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.apiValue

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.fromApiValue(value)
}
