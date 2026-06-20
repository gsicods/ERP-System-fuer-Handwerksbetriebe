package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.AnfrageDokument
import org.example.kalkulationsprogramm.domain.AnfrageGeschaeftsdokument
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AnfrageDokumentRepository : JpaRepository<AnfrageDokument, Long> {
    fun findByAnfrageId(anfrageId: Long?): List<AnfrageDokument>

    @Query("SELECT g FROM AnfrageGeschaeftsdokument g")
    fun findAllGeschaeftsdokumente(): List<AnfrageGeschaeftsdokument>

    @Query("SELECT g FROM AnfrageGeschaeftsdokument g WHERE g.geschaeftsdokumentart = 'Rechnung'")
    fun findOffeneGeschaeftsdokumente(): List<AnfrageGeschaeftsdokument>

    fun findByGespeicherterDateiname(gespeicherterDateiname: String): Optional<AnfrageDokument>

    fun findByGespeicherterDateinameIgnoreCase(gespeicherterDateiname: String): Optional<AnfrageDokument>

    @Query("SELECT g.id, g.anfrage.id FROM AnfrageGeschaeftsdokument g WHERE g.anfrage.id IN :anfrageIds")
    fun findGeschaeftsdokumentIdMappingByAnfrageIds(
        @Param("anfrageIds") anfrageIds: List<Long>,
    ): List<Array<Any>>
}
