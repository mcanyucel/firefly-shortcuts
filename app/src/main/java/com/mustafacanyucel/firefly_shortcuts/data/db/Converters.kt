package com.mustafacanyucel.firefly_shortcuts.data.db

import androidx.room.TypeConverter
import com.mustafacanyucel.firefly_shortcuts.model.TransactionType
import com.mustafacanyucel.firefly_shortcuts.sms.SmsTemplate
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.apiValue

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.fromApiValue(value)

    @TypeConverter
    fun fromSmsTemplate(template: SmsTemplate): String = templateJson.encodeToString(SmsTemplate.serializer(), template)

    @TypeConverter
    fun toSmsTemplate(value: String): SmsTemplate = templateJson.decodeFromString(SmsTemplate.serializer(), value)

    private companion object {
        // Tolerates fields added by a newer version.
        val templateJson = Json { ignoreUnknownKeys = true }
    }
}
