package org.example.kalkulationsprogramm.domain

enum class Qualifikation(val bezeichnung: String) {
    AUSZUBILDENDER("Auszubildender"),
    FACHARBEITER("Facharbeiter"),
    MEISTER("Meister");

    companion object {
        @JvmStatic
        fun fromString(value: String?): Qualifikation? {
            if (value.isNullOrBlank()) return null
            val normalized = value.trim().uppercase()
            return entries.firstOrNull {
                it.name.equals(normalized, ignoreCase = true) || it.bezeichnung.equals(value.trim(), ignoreCase = true)
            }
        }
    }
}
