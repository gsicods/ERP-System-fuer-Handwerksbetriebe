package org.example.kalkulationsprogramm.dto.Anfrage

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AnfrageFunnelRequestDto(
    @field:NotBlank
    @field:Size(max = 50)
    var serviceTyp: String? = null,
    @field:Size(max = 20)
    var projektarten: List<@Size(max = 100) String>? = null,
    @field:NotBlank
    @field:Size(max = 5000)
    var nachricht: String? = null,
    @field:NotBlank
    @field:Size(max = 100)
    var vorname: String? = null,
    @field:NotBlank
    @field:Size(max = 100)
    var nachname: String? = null,
    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    var email: String? = null,
    @field:Size(max = 50)
    var telefon: String? = null,
    @field:Size(max = 500)
    var projektAnschrift: String? = null,
    @field:Size(max = 500)
    var rechnungsAnschrift: String? = null,
    var isRechnungsAnschriftGleichProjekt: Boolean = false,
    @get:AssertTrue(message = "Datenschutz muss akzeptiert werden")
    var isDatenschutzAkzeptiert: Boolean = false,
    @field:Size(max = 50)
    var consentIp: String? = null,
    @field:Size(max = 50)
    var datenschutzVersion: String? = null,
)
