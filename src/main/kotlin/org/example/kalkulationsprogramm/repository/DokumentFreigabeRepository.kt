package org.example.kalkulationsprogramm.repository

import java.time.LocalDateTime
import java.util.Optional
import org.example.kalkulationsprogramm.domain.DokumentFreigabe
import org.example.kalkulationsprogramm.domain.FreigabeQuellTyp
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DokumentFreigabeRepository : JpaRepository<DokumentFreigabe, Long> {
    fun findByUuid(uuid: String): Optional<DokumentFreigabe>

    @Query(
        """
            SELECT f FROM DokumentFreigabe f
            WHERE f.quellTyp = :quellTyp AND f.quellDokumentId IN :ids
            """,
    )
    fun findByQuelle(
        @Param("quellTyp") quellTyp: FreigabeQuellTyp,
        @Param("ids") ids: List<Long>,
    ): List<DokumentFreigabe>

    @Query(
        """
            SELECT f FROM DokumentFreigabe f
            WHERE f.status = org.example.kalkulationsprogramm.domain.FreigabeStatus.ACCEPTED
              AND f.akzeptiertAm IS NOT NULL
              AND f.akzeptiertAm >= :seit
            ORDER BY f.akzeptiertAm DESC
            """,
    )
    fun findKuerzlichAkzeptiert(@Param("seit") seit: LocalDateTime): List<DokumentFreigabe>
}
