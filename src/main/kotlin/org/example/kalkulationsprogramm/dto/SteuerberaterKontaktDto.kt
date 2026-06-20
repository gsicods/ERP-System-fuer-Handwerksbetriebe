package org.example.kalkulationsprogramm.dto

import java.time.LocalDate

data class SteuerberaterKontaktDto(
    var id: Long? = null,
    var name: String? = null,
    var email: String? = null,
    var telefon: String? = null,
    var ansprechpartner: String? = null,
    var autoProcessEmails: Boolean? = null,
    var aktiv: Boolean? = null,
    var notizen: String? = null,
    var gueltigAb: LocalDate? = null,
    var gueltigBis: LocalDate? = null,
    var weitereEmails: List<String>? = null,
    var ansprechpartnerListe: List<SteuerberaterAnsprechpartnerDto>? = null,
)
