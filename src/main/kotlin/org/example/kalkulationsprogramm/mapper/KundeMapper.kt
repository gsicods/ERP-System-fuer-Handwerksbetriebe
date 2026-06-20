package org.example.kalkulationsprogramm.mapper

import org.example.kalkulationsprogramm.domain.Kunde
import org.example.kalkulationsprogramm.dto.Kunde.KundeListItemDto
import org.example.kalkulationsprogramm.dto.Kunde.KundeResponseDto
import org.springframework.stereotype.Component

@Component
class KundeMapper {
    fun toListItem(kunde: Kunde?): KundeListItemDto? {
        if (kunde == null) {
            return null
        }
        val dto = KundeListItemDto()
        dto.id = kunde.id
        dto.kundennummer = kunde.kundennummer
        dto.name = kunde.name
        dto.anrede = kunde.anrede?.name
        dto.ansprechspartner = kunde.ansprechspartner
        dto.strasse = kunde.strasse
        dto.plz = kunde.plz
        dto.ort = kunde.ort
        dto.telefon = kunde.telefon
        dto.mobiltelefon = kunde.mobiltelefon
        dto.kundenEmails = kunde.kundenEmails
        dto.isHatProjekte = !kunde.projekts.isNullOrEmpty()
        return dto
    }

    fun toResponseDto(kunde: Kunde?): KundeResponseDto? {
        if (kunde == null) {
            return null
        }
        val dto = KundeResponseDto()
        dto.id = kunde.id
        dto.kundennummer = kunde.kundennummer
        dto.name = kunde.name
        dto.anrede = kunde.anrede?.name
        dto.ansprechspartner = kunde.ansprechspartner
        dto.strasse = kunde.strasse
        dto.plz = kunde.plz
        dto.ort = kunde.ort
        dto.telefon = kunde.telefon
        dto.mobiltelefon = kunde.mobiltelefon
        dto.kundenEmails = kunde.kundenEmails
        return dto
    }
}
