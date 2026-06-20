package org.example.kalkulationsprogramm.dto

import org.example.kalkulationsprogramm.domain.ReklamationStatus

data class CreateReklamationRequest(
    var beschreibung: String? = null,
    var lieferscheinId: Long? = null,
    var status: ReklamationStatus? = null,
)
