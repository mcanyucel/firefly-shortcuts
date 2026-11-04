package com.mustafacanyucel.firefly_shortcuts.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Firefly uses JSON:API: each object is {"id": ..., "attributes": {...}},
// and lists come in pages,

@Serializable
data class Resource<A>(val id:String, val attributes: A)

@Serializable
data class Page<A>(val data: List<Resource<A>>, val meta: Meta)

@Serializable
data class Meta(val pagination: Pagination)

@Serializable
data class Pagination(
    @SerialName("current_page") val currentPage: Int,
    @SerialName("total_pages") val totalPages: Int,
)

@Serializable
data class SingleResponse(val data: ResourceId)

@Serializable
data class ResourceId(val id: String)

@Serializable
data class AccountAttributes(
    val name: String,
    val type: String,
    @SerialName("account_role") val accountRole: String? = null,
    @SerialName("currency_code") val currencyCode: String?  = null,
    @SerialName("currency_symbol") val currencySymbol: String? = null,
    val active: Boolean = true
)

/** Bills, budgets, categories, and piggy banks */
@Serializable
data class NamedAttributes(val name: String)

@Serializable
data class TagAttributes(val tag: String)
