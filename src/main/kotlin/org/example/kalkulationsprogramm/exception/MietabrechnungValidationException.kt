package org.example.kalkulationsprogramm.exception

import org.springframework.http.HttpStatus

class MietabrechnungValidationException(
    val status: HttpStatus,
    val userMessage: String,
    val detail: String?
) : RuntimeException(detail ?: userMessage) {
    constructor(userMessage: String, detail: String?) : this(HttpStatus.BAD_REQUEST, userMessage, detail)
}
