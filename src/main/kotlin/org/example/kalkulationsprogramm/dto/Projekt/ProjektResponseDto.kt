package org.example.kalkulationsprogramm.dto.Projekt

import org.example.kalkulationsprogramm.dto.Anfrage.AnfrageResponseDto
import org.example.kalkulationsprogramm.dto.Artikel.ArtikelInProjektResponseDto
import org.example.kalkulationsprogramm.dto.Kunde.KundeResponseDto
import org.example.kalkulationsprogramm.dto.Materialkosten.MaterialkostenResponseDto
import org.example.kalkulationsprogramm.dto.ProjektEmail.ProjektEmailDto
import org.example.kalkulationsprogramm.dto.ProjektProduktkategorie.ProjektProduktkategorieResponseDto
import org.example.kalkulationsprogramm.dto.ProjektZeit.ZeitResponseDto
import java.math.BigDecimal
import java.time.LocalDate

data class ProjektResponseDto(
    var id: Long? = null,
    var bauvorhaben: String? = null,
    var strasse: String? = null,
    var plz: String? = null,
    var ort: String? = null,
    var kunde: String? = null,
    var kundeDto: KundeResponseDto? = null,
    var kundenId: Long? = null,
    var kundennummer: String? = null,
    var kundenEmails: List<String>? = null,
    var kurzbeschreibung: String? = null,
    var auftragsnummer: String? = null,
    var anlegedatum: LocalDate? = null,
    var abschlussdatum: LocalDate? = null,
    var bildUrl: String? = null,
    var bruttoPreis: BigDecimal? = null,
    var materialkosten: List<MaterialkostenResponseDto>? = null,
    var artikel: List<ArtikelInProjektResponseDto>? = null,
    var kilogrammProMaterial: List<MaterialKilogrammDto>? = null,
    var gesamtKilogramm: BigDecimal? = null,
    var isBezahlt: Boolean = false,
    var isAbgeschlossen: Boolean = false,
    var projektArt: String? = null,
    var isProduktiv: Boolean = false,
    var produktkategorien: List<ProjektProduktkategorieResponseDto>? = null,
    var zeiten: List<ZeitResponseDto>? = null,
    var anfragen: List<AnfrageResponseDto>? = null,
    var emails: List<ProjektEmailDto>? = null,
)
