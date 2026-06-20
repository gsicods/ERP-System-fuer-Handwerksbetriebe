package org.example.kalkulationsprogramm.dto.Kunde

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

open class KundeCreateRequestDto {
    @field:Size(max = 64, message = "Kundennummer ist zu lang.")
    var kundennummer: String? = null
    @field:NotBlank(message = "Name darf nicht leer sein.")
    @field:Size(max = 255, message = "Name ist zu lang.")
    var name: String? = null
    @field:Size(max = 20, message = "Anrede ist zu lang.")
    var anrede: String? = null
    @field:Size(max = 255, message = "Ansprechpartner ist zu lang.")
    var ansprechspartner: String? = null
    @field:Size(max = 255, message = "Straße ist zu lang.")
    var strasse: String? = null
    @field:Size(max = 20, message = "PLZ ist zu lang.")
    var plz: String? = null
    @field:Size(max = 255, message = "Ort ist zu lang.")
    var ort: String? = null
    @field:Size(max = 50, message = "Telefonnummer ist zu lang.")
    var telefon: String? = null
    @field:Size(max = 50, message = "Mobiltelefon ist zu lang.")
    var mobiltelefon: String? = null
    var zahlungsziel: Int? = null
    var kundenEmails: List<@Email(message = "Ungültige E-Mail-Adresse.") @Size(max = 255, message = "E-Mail ist zu lang.") String>? = null
        set(value) {
            field = value?.mapNotNull { email ->
                email.trim().takeIf { it.isNotEmpty() }
            }
        }
}
