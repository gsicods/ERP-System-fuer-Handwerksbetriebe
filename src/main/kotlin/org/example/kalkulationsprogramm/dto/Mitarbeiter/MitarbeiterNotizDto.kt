package org.example.kalkulationsprogramm.dto.Mitarbeiter
import java.time.LocalDateTime
data class MitarbeiterNotizDto(
    val id: Long,
    val inhalt: String,
    val erstelltAm: LocalDateTime,
    val mitarbeiterId: Long
) {
    fun id(): Long = id
    fun inhalt(): String = inhalt
    fun erstelltAm(): LocalDateTime = erstelltAm
    fun mitarbeiterId(): Long = mitarbeiterId
}
