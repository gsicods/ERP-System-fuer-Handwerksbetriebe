package org.example.kalkulationsprogramm.dto.Artikel

import org.example.kalkulationsprogramm.domain.Verrechnungseinheit
import java.math.BigDecimal
import java.util.Date

data class ArtikelResponseDto(
    var id: Long? = null,
    var externeArtikelnummer: String? = null,
    var produktlinie: String? = null,
    var produktname: String? = null,
    var produkttext: String? = null,
    var verpackungseinheit: Long? = null,
    var preiseinheit: String? = null,
    var verrechnungseinheit: Verrechnungseinheit? = null,
    var waehrung: String? = null,
    var preis: BigDecimal? = null,
    var kgProMeter: BigDecimal? = null,
    var preisDatum: Date? = null,
    var lieferantId: Long? = null,
    var lieferantenname: String? = null,
    var lieferantenpreise: List<LieferantPreisDto>? = null,
    var kategorieId: Long? = null,
    var kategoriePfad: String? = null,
    var parentKategorieId: Long? = null,
    var rootKategorieId: Long? = null,
    var rootKategorieName: String? = null,
    var werkstoffId: Long? = null,
    var werkstoffName: String? = null,
    var kommentar: String? = null,
    var isMeterware: Boolean = false,
)
