package org.example.kalkulationsprogramm.dto

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class ApiError(
    val status: Int,
    val message: String?,
    val constraint: String?,
    val fields: List<Field>?,
    val detail: String?
) {
    fun status(): Int = status
    fun message(): String? = message
    fun constraint(): String? = constraint
    fun fields(): List<Field>? = fields
    fun detail(): String? = detail

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class Field(
        val field: String?,
        val label: String?,
        val message: String?
    ) {
        fun field(): String? = field
        fun label(): String? = label
        fun message(): String? = message
    }
}
