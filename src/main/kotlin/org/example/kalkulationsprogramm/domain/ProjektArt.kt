package org.example.kalkulationsprogramm.domain

enum class ProjektArt(
    private val produktiv: Boolean,
    val displayName: String
) {
    PAUSCHAL(true, "Pauschalpreis"),
    REGIE(true, "Regie"),
    INTERN(false, "Internes Projekt"),
    GARANTIE(false, "Garantie");

    fun isProduktiv(): Boolean = produktiv
}
