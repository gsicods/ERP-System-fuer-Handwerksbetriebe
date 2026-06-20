package org.example.kalkulationsprogramm.dto.Lieferant
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
data class LieferantArtikelpreisCreateRequest(
    @field:NotNull val artikelId: Long,
    @field:NotNull @field:DecimalMin(value = "0", inclusive = true) val preis: BigDecimal,
    val externeArtikelnummer: String
) {
    fun artikelId(): Long = artikelId
    fun preis(): BigDecimal = preis
    fun externeArtikelnummer(): String = externeArtikelnummer
}
