package org.example.kalkulationsprogramm.dto.Verrechnungslohn

import java.math.BigDecimal

class VerrechnungslohnErgebnisDto {
    enum class Modus { RUECKWIRKEND, HOCHRECHNUNG }
    enum class LohnQuelle { LOHNABRECHNUNG, KALKULATORISCH, STUNDENLOHN_HOCHRECHNUNG, STAMMSTUNDENLOHN }

    var jahr: Int = 0
    var modus: Modus? = null
    var lohnzeilen: MutableList<MitarbeiterLohnZeile> = ArrayList()
    var stundenzeilen: MutableList<MitarbeiterStundenZeile> = ArrayList()
    var kostenstellen: MutableList<KostenstelleAnteil> = ArrayList()
    var abteilungen: MutableList<AbteilungVorschlag> = ArrayList()
    var datenLuecken: MutableList<DatenLuecke> = ArrayList()
    var lohnsummeGesamt: BigDecimal = BigDecimal.ZERO
    var verkaeuflicheStundenGesamt: BigDecimal = BigDecimal.ZERO
    var gemeinkostenGesamt: BigDecimal = BigDecimal.ZERO
    var selbstkostenProStunde: BigDecimal = BigDecimal.ZERO

    data class MitarbeiterLohnZeile(
        var mitarbeiterId: Long? = null,
        var name: String? = null,
        var isIstGeschaeftsfuehrer: Boolean = false,
        var beschaeftigungsart: String? = null,
        var bruttoJahr: BigDecimal = BigDecimal.ZERO,
        var agAnteilSv: BigDecimal = BigDecimal.ZERO,
        var bgBeitrag: BigDecimal = BigDecimal.ZERO,
        var geldwerterVorteilJahr: BigDecimal = BigDecimal.ZERO,
        var gesamtkosten: BigDecimal = BigDecimal.ZERO,
        var quelle: LohnQuelle? = null,
        var isBruttoIstDefault: Boolean = false,
    )
    data class MitarbeiterStundenZeile(
        var mitarbeiterId: Long? = null,
        var name: String? = null,
        var isIstGeschaeftsfuehrer: Boolean = false,
        var sollstunden: BigDecimal = BigDecimal.ZERO,
        var urlaubsstunden: BigDecimal = BigDecimal.ZERO,
        var krankheitsstunden: BigDecimal = BigDecimal.ZERO,
        var interneStunden: BigDecimal = BigDecimal.ZERO,
        var feiertagsstunden: BigDecimal = BigDecimal.ZERO,
        var verkaeuflicheStunden: BigDecimal = BigDecimal.ZERO,
        var isUrlaubIstDefault: Boolean = false,
        var isKrankheitIstDefault: Boolean = false,
        var isInterneIstDefault: Boolean = false,
    )
    data class KostenstelleAnteil(var kostenstelleId: Long? = null, var bezeichnung: String? = null, var jahresbetrag: BigDecimal = BigDecimal.ZERO, var isGestreckt: Boolean = false)
    data class AbteilungVorschlag(var abteilungId: Long? = null, var name: String? = null, var aufschlagEuro: BigDecimal = BigDecimal.ZERO)
    data class DatenLuecke(var mitarbeiterId: Long? = null, var mitarbeiterName: String? = null, var problem: String? = null)
}
