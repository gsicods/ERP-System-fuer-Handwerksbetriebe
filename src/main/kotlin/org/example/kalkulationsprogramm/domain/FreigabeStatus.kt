package org.example.kalkulationsprogramm.domain

enum class FreigabeStatus(val label: String) {
    PENDING("Wartet auf Kunde"),
    ACCEPTED("Angenommen"),
    EXPIRED("Abgelaufen"),
    REVOKED("Zurückgezogen")
}
