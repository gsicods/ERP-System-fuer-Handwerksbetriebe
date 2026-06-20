package org.example.kalkulationsprogramm.domain

import java.time.LocalDate

interface Dokument {
    fun getId(): Long?
    fun getOriginalDateiname(): String?
    fun getGespeicherterDateiname(): String?
    fun getDateityp(): String?
    fun getDateigroesse(): Long?
    fun getUploadDatum(): LocalDate?
    fun getEmailVersandDatum(): LocalDate?
    fun getDokumentGruppe(): DokumentGruppe?
}
