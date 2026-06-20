package org.example.kalkulationsprogramm.dto.Mitarbeiter

import java.math.BigDecimal
import java.time.LocalDate

data class MitarbeiterDto(
    var id: Long? = null,
    var vorname: String? = null,
    var nachname: String? = null,
    var strasse: String? = null,
    var plz: String? = null,
    var ort: String? = null,
    var email: String? = null,
    var telefon: String? = null,
    var festnetz: String? = null,
    var qualifikation: String? = null,
    var stundenlohn: BigDecimal? = null,
    var jahresUrlaub: Int? = null,
    var geburtstag: LocalDate? = null,
    var eintrittsdatum: LocalDate? = null,
    var aktiv: Boolean? = null,
    var abteilungIds: List<Long>? = null,
    var abteilungNames: String? = null,
    var loginToken: String? = null,
    var beschaeftigungsart: String? = null,
    var beschaeftigungsartLabel: String? = null,
    var krankenkasseId: Long? = null,
    var krankenkasseName: String? = null,
    var kinderlos: Boolean? = null,
    var istGeschaeftsfuehrer: Boolean? = null,
    var kalkulatorischerLohnMonat: BigDecimal? = null,
    var geldwertVorteilMonat: BigDecimal? = null,
)
