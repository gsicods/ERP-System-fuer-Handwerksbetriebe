package org.example.kalkulationsprogramm.dto

import org.example.kalkulationsprogramm.domain.LieferantDokumentTyp

class AbteilungBerechtigungDto {
    data class Response(
        var abteilungId: Long? = null,
        var abteilungName: String? = null,
        var berechtigungen: List<TypBerechtigung>? = null,
        var darfRechnungenGenehmigen: Boolean? = null,
        var darfRechnungenSehen: Boolean? = null,
        var darfFreigabeAnnahmePushen: Boolean? = null,
        var darfWebseitenAnfragenPushen: Boolean? = null,
    ) {
        companion object {
            @JvmStatic fun builder(): Builder = Builder()
        }

        class Builder {
            private var abteilungId: Long? = null
            private var abteilungName: String? = null
            private var berechtigungen: List<TypBerechtigung>? = null
            private var darfRechnungenGenehmigen: Boolean? = null
            private var darfRechnungenSehen: Boolean? = null
            private var darfFreigabeAnnahmePushen: Boolean? = null
            private var darfWebseitenAnfragenPushen: Boolean? = null
            fun abteilungId(abteilungId: Long?) = apply { this.abteilungId = abteilungId }
            fun abteilungName(abteilungName: String?) = apply { this.abteilungName = abteilungName }
            fun berechtigungen(berechtigungen: List<TypBerechtigung>?) = apply { this.berechtigungen = berechtigungen }
            fun darfRechnungenGenehmigen(darfRechnungenGenehmigen: Boolean?) = apply { this.darfRechnungenGenehmigen = darfRechnungenGenehmigen }
            fun darfRechnungenSehen(darfRechnungenSehen: Boolean?) = apply { this.darfRechnungenSehen = darfRechnungenSehen }
            fun darfFreigabeAnnahmePushen(darfFreigabeAnnahmePushen: Boolean?) = apply { this.darfFreigabeAnnahmePushen = darfFreigabeAnnahmePushen }
            fun darfWebseitenAnfragenPushen(darfWebseitenAnfragenPushen: Boolean?) = apply { this.darfWebseitenAnfragenPushen = darfWebseitenAnfragenPushen }
            fun build() = Response(abteilungId, abteilungName, berechtigungen, darfRechnungenGenehmigen, darfRechnungenSehen, darfFreigabeAnnahmePushen, darfWebseitenAnfragenPushen)
        }
    }

    data class TypBerechtigung(
        var typ: LieferantDokumentTyp? = null,
        var darfSehen: Boolean? = null,
        var darfScannen: Boolean? = null,
    ) {
        companion object {
            @JvmStatic fun builder(): Builder = Builder()
        }

        class Builder {
            private var typ: LieferantDokumentTyp? = null
            private var darfSehen: Boolean? = null
            private var darfScannen: Boolean? = null
            fun typ(typ: LieferantDokumentTyp?) = apply { this.typ = typ }
            fun darfSehen(darfSehen: Boolean?) = apply { this.darfSehen = darfSehen }
            fun darfScannen(darfScannen: Boolean?) = apply { this.darfScannen = darfScannen }
            fun build() = TypBerechtigung(typ, darfSehen, darfScannen)
        }
    }

    data class UpdateRequest(
        var berechtigungen: List<TypBerechtigung>? = null,
        var darfRechnungenGenehmigen: Boolean? = null,
        var darfRechnungenSehen: Boolean? = null,
        var darfFreigabeAnnahmePushen: Boolean? = null,
        var darfWebseitenAnfragenPushen: Boolean? = null,
    )
}
