package org.example.kalkulationsprogramm.dto.Kunde

data class KundeDuplikatTrefferDto(
    var id: Long? = null,
    var kundennummer: String? = null,
    var name: String? = null,
    var ansprechspartner: String? = null,
    var strasse: String? = null,
    var plz: String? = null,
    var ort: String? = null,
    var telefon: String? = null,
    var mobiltelefon: String? = null,
    var kundenEmails: List<String>? = null,
    var score: Int = 0,
    var gruende: List<KundeDuplikatGrund>? = null,
)
