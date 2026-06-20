package org.example.kalkulationsprogramm.dto.Email

data class EmailMoveRequest(
    var targetType: String? = null,
    var targetId: Long? = null,
)
