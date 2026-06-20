package org.example.kalkulationsprogramm.domain

enum class BelegKategorie {
    UNZUGEORDNET,
    KASSE_EINNAHME,
    KASSE_AUSGABE,
    PRIVATENTNAHME,
    PRIVATEINLAGE,
    BANK,
    KREDITKARTE,
    SONSTIGER_BELEG;

    fun istKassenBewegung(): Boolean =
        this == KASSE_EINNAHME || this == KASSE_AUSGABE || this == PRIVATENTNAHME || this == PRIVATEINLAGE

    fun istAusgang(): Boolean =
        this == KASSE_AUSGABE || this == PRIVATENTNAHME
}
