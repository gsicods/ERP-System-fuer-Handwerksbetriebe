package org.example.kalkulationsprogramm.util

import org.springframework.http.HttpStatus
class ConstraintErrorDetail(
    status: HttpStatus,
    val userMessage: String?,
    val technicalMessage: String?,
    val constraintName: String?,
    fieldErrors: kotlin.collections.List<FieldErrorDetail>?
) {
    val status: HttpStatus = requireNotNull(status) { "Der Status darf nicht null sein." }
    val fieldErrors: kotlin.collections.List<FieldErrorDetail> = fieldErrors?.toList() ?: emptyList()

    fun status(): HttpStatus = status
    fun userMessage(): String? = userMessage
    fun technicalMessage(): String? = technicalMessage
    fun constraintName(): String? = constraintName
    fun fieldErrors(): kotlin.collections.List<FieldErrorDetail> = fieldErrors
}
