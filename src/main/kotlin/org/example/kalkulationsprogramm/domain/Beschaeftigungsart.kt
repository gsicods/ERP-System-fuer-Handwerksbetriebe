package org.example.kalkulationsprogramm.domain

enum class Beschaeftigungsart(val bezeichnung: String) {
    REGULAER("Regulaer sozialversicherungspflichtig"),
    MINIJOB("Minijob (geringfuegig beschaeftigt)"),
    GF_SV_PFLICHTIG("Geschaeftsfuehrer (sozialversicherungspflichtig)"),
    GF_SV_FREI("Geschaeftsfuehrer (sozialversicherungsfrei)")
}
