package org.example.kalkulationsprogramm.dto.Projekt
data class LieferantPerformanceDto(
    val name: String,
    val bestellungen: Int,
    val netto: Double
) {
    fun name(): String = name
    fun bestellungen(): Int = bestellungen
    fun netto(): Double = netto
}
