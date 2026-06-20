package org.example.kalkulationsprogramm.util

import java.util.Objects

data class FieldErrorDetail(
    val field: String?,
    val label: String?,
    val message: String
) {
    init {
        if (field.isNullOrBlank() && label.isNullOrBlank()) {
            throw IllegalArgumentException("Es muss entweder ein Feldname oder ein Label angegeben werden.")
        }
        Objects.requireNonNull(message, "Die Fehlermeldung darf nicht null sein.")
    }

    fun field(): String? = field
    fun label(): String? = label
    fun message(): String = message
}
