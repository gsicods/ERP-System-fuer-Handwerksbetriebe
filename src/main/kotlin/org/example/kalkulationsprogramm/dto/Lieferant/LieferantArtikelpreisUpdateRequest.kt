package org.example.kalkulationsprogramm.dto.Lieferant
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
data class LieferantArtikelpreisUpdateRequest(
    @field:NotNull @field:DecimalMin(value = "0", inclusive = true) val preis: BigDecimal,
    val externeArtikelnummer: String
) {
    fun preis(): BigDecimal = preis
    fun externeArtikelnummer(): String = externeArtikelnummer
}
