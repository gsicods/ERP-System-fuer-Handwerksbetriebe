package org.example.kalkulationsprogramm.domain

enum class TextbausteinTyp {
    VORTEXT,
    NACHTEXT,
    ZAHLUNGSZIEL,
    FREITEXT;

    companion object {
        @JvmStatic
        fun fromString(value: String?): TextbausteinTyp =
            when (value?.trim()?.uppercase()) {
                "VORTEXT" -> VORTEXT
                "NACHTEXT" -> NACHTEXT
                "ZAHLUNGSZIEL" -> ZAHLUNGSZIEL
                else -> FREITEXT
            }
    }
}
