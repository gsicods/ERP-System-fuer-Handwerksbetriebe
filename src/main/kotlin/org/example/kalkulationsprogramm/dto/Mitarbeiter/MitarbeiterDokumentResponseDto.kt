package org.example.kalkulationsprogramm.dto.Mitarbeiter

import org.example.kalkulationsprogramm.domain.DokumentGruppe
import java.time.LocalDate

data class MitarbeiterDokumentResponseDto(
    var id: Long? = null,
    var originalDateiname: String? = null,
    var dateityp: String? = null,
    var dateigroesse: Long? = null,
    var uploadDatum: LocalDate? = null,
    var dokumentGruppe: DokumentGruppe? = null,
    var url: String? = null,
)
