package org.example.kalkulationsprogramm.dto.Freigabe

import java.time.LocalDateTime

data class FreigabeStatusKurzDto(
    val status: String? = null,
    val dokumentArt: String? = null,
    val dokumentNummer: String? = null,
    val akzeptiertAm: LocalDateTime? = null,
    val ablaufDatum: LocalDateTime? = null,
    val erstelltAm: LocalDateTime? = null,
) {
    companion object {
        @JvmStatic fun builder(): Builder = Builder()
    }

    class Builder {
        private var status: String? = null
        private var dokumentArt: String? = null
        private var dokumentNummer: String? = null
        private var akzeptiertAm: LocalDateTime? = null
        private var ablaufDatum: LocalDateTime? = null
        private var erstelltAm: LocalDateTime? = null
        fun status(status: String?) = apply { this.status = status }
        fun dokumentArt(dokumentArt: String?) = apply { this.dokumentArt = dokumentArt }
        fun dokumentNummer(dokumentNummer: String?) = apply { this.dokumentNummer = dokumentNummer }
        fun akzeptiertAm(akzeptiertAm: LocalDateTime?) = apply { this.akzeptiertAm = akzeptiertAm }
        fun ablaufDatum(ablaufDatum: LocalDateTime?) = apply { this.ablaufDatum = ablaufDatum }
        fun erstelltAm(erstelltAm: LocalDateTime?) = apply { this.erstelltAm = erstelltAm }
        fun build() = FreigabeStatusKurzDto(status, dokumentArt, dokumentNummer, akzeptiertAm, ablaufDatum, erstelltAm)
    }
}
