package org.example.kalkulationsprogramm.dto.Freigabe

import java.time.LocalDateTime

data class FreigabeAkzeptiertResponse(
    val uuid: String? = null,
    val dokumentNummer: String? = null,
    val dokumentArt: String? = null,
    val akzeptiertAm: LocalDateTime? = null,
    val hashAcceptance: String? = null,
    val unterzeichnerName: String? = null,
) {
    companion object {
        @JvmStatic fun builder(): Builder = Builder()
    }

    class Builder {
        private var uuid: String? = null
        private var dokumentNummer: String? = null
        private var dokumentArt: String? = null
        private var akzeptiertAm: LocalDateTime? = null
        private var hashAcceptance: String? = null
        private var unterzeichnerName: String? = null
        fun uuid(uuid: String?) = apply { this.uuid = uuid }
        fun dokumentNummer(dokumentNummer: String?) = apply { this.dokumentNummer = dokumentNummer }
        fun dokumentArt(dokumentArt: String?) = apply { this.dokumentArt = dokumentArt }
        fun akzeptiertAm(akzeptiertAm: LocalDateTime?) = apply { this.akzeptiertAm = akzeptiertAm }
        fun hashAcceptance(hashAcceptance: String?) = apply { this.hashAcceptance = hashAcceptance }
        fun unterzeichnerName(unterzeichnerName: String?) = apply { this.unterzeichnerName = unterzeichnerName }
        fun build() = FreigabeAkzeptiertResponse(uuid, dokumentNummer, dokumentArt, akzeptiertAm, hashAcceptance, unterzeichnerName)
    }
}
