package com.mustafacanyucel.firefly_shortcuts.model

enum class TransactionType(val apiValue: String) {
    WITHDRAWAL("withdrawal"),
    DEPOSIT("deposit"),
    TRANSFER("transfer");

    companion object {
        fun fromApiValue(value: String): TransactionType =
            entries.firstOrNull { it.apiValue == value.lowercase() } ?: WITHDRAWAL
    }
}
