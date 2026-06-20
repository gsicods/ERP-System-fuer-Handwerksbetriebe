package org.example.kalkulationsprogramm.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class Verrechnungseinheit(val anzeigename: String) {
    LAUFENDE_METER("Laufende Meter"),
    QUADRATMETER("Quadratmeter"),
    KILOGRAMM("Kilogramm"),
    STUECK("Stueck");

    fun getName(): String = name

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(value: String): Verrechnungseinheit =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unbekannte Verrechnungseinheit: $value")
    }
}
