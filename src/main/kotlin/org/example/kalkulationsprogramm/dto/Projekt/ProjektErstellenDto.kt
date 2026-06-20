package org.example.kalkulationsprogramm.dto.Projekt

import org.example.kalkulationsprogramm.dto.Materialkosten.MaterialkostenErfassenDto
import org.example.kalkulationsprogramm.dto.ProjektProduktkategorie.ProjektProduktkategorieErfassenDto
import org.example.kalkulationsprogramm.dto.ProjektZeit.ZeitErfassenDto
import java.math.BigDecimal
import java.time.LocalDate

data class ProjektErstellenDto(
    var bauvorhaben: String? = null,
    var kunde: String? = null,
    var kundennummer: String? = null,
    var kundenId: Long? = null,
    var strasse: String? = null,
    var plz: String? = null,
    var ort: String? = null,
    var kundenEmails: List<String>? = null,
    var kurzbeschreibung: String? = null,
    var auftragsnummer: String? = null,
    var anlegedatum: LocalDate? = null,
    var abschlussdatum: LocalDate? = null,
    var bruttoPreis: BigDecimal? = null,
    var materialkosten: List<MaterialkostenErfassenDto>? = null,
    var isBezahlt: Boolean = false,
    var isAbgeschlossen: Boolean = false,
    var projektArt: String? = null,
    var anfrageIds: List<Long>? = null,
    var angebotIds: List<Long>? = null,
    var zeitPositionen: List<ZeitErfassenDto>? = null,
    var produktkategorien: List<ProjektProduktkategorieErfassenDto>? = null,
)
