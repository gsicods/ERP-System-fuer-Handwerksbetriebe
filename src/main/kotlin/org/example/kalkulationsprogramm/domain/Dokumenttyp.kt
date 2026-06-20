package org.example.kalkulationsprogramm.domain

enum class Dokumenttyp(val label: String) {
    ANGEBOT("Angebot"),
    NACHTRAGSANGEBOT("Nachtragsangebot"),
    AUFTRAGSBESTAETIGUNG("Auftragsbestätigung"),
    RECHNUNG("Rechnung"),
    TEILRECHNUNG("Teilrechnung"),
    ABSCHLAGSRECHNUNG("Abschlagsrechnung"),
    SCHLUSSRECHNUNG("Schlussrechnung"),
    ZAHLUNGSERINNERUNG("Zahlungserinnerung"),
    ERSTE_MAHNUNG("1. Mahnung"),
    ZWEITE_MAHNUNG("2. Mahnung"),
    STORNORECHNUNG("Stornorechnung"),
    GUTSCHRIFT("Gutschrift");

    companion object {
        @JvmStatic
        fun fromLabel(label: String?): Dokumenttyp? {
            if (label == null) return null
            val trimmed = label.trim()
            entries.firstOrNull { it.label.equals(trimmed, ignoreCase = true) }?.let { return it }
            return try {
                valueOf(trimmed.uppercase())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Unbekannter Dokumenttyp: $label")
            }
        }
    }
}
