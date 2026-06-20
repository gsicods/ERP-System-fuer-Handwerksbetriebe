package org.example.kalkulationsprogramm.dto

import org.example.kalkulationsprogramm.controller.LieferantenController
import org.example.kalkulationsprogramm.domain.ReklamationStatus
import java.time.LocalDateTime

data class LieferantReklamationDto(
    var id: Long? = null,
    var lieferantId: Long? = null,
    var lieferantName: String? = null,
    var lieferscheinId: Long? = null,
    var lieferscheinNummer: String? = null,
    var lieferscheinDateiname: String? = null,
    var erstellerName: String? = null,
    var erstelltAm: LocalDateTime? = null,
    var beschreibung: String? = null,
    var status: ReklamationStatus? = null,
    var bilder: List<LieferantenController.LieferantBildDto>? = null,
)
