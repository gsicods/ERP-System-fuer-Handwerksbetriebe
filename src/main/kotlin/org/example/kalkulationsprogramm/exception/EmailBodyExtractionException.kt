package org.example.kalkulationsprogramm.exception

class EmailBodyExtractionException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
