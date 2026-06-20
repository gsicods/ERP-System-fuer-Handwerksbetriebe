package org.example.kalkulationsprogramm.domain

enum class Anrede {
    HERR,
    FRAU,
    FAMILIE,
    FIRMA,
    DAMEN_HERREN;

    fun toAnredeText(): String =
        when (this) {
            HERR -> "Sehr geehrter Herr"
            FRAU -> "Sehr geehrte Frau"
            FAMILIE -> "Sehr geehrte Familie"
            FIRMA, DAMEN_HERREN -> "Sehr geehrte Damen und Herren"
        }

    companion object {
        @JvmStatic
        fun fromString(value: String?): Anrede? =
            try {
                value?.trim()?.uppercase()?.let { valueOf(it) }
            } catch (_: Exception) {
                null
            }
    }
}
