package org.example.kalkulationsprogramm.dto.Projekt
data class LieferantenkostenJahrDto(
    val jahr: Int,
    val bestellungen: Int,
    val netto: Double
) {
    fun jahr(): Int = jahr
    fun bestellungen(): Int = bestellungen
    fun netto(): Double = netto
}
