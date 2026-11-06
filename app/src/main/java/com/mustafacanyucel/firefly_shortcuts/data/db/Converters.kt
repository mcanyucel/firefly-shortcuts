package com.mustafacanyucel.firefly_shortcuts.data.db

import androidx.room.TypeConverter
import com.mustafacanyucel.firefly_shortcuts.model.TransactionType
import com.mustafacanyucel.firefly_shortcuts.sms.SmsTemplate
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.apiValue

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.fromApiValue(value)

    @TypeConverter
    fun fromSmsTemplate(template: SmsTemplate): String = templateJson.encodeToString(SmsTemplate.serializer(), template)

    @TypeConverter
    fun toSmsTemplate(value: String): SmsTemplate = templateJson.decodeFromString(SmsTemplate.serializer(), value)

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? = time?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    private companion object {
        // Tolerates fields added by a newer version.
        val templateJson = Json { ignoreUnknownKeys = true }
    }
}
