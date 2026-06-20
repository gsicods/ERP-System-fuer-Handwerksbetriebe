package org.example.kalkulationsprogramm.repository

import java.math.BigDecimal
import java.util.Optional
import org.example.kalkulationsprogramm.domain.BwaTyp
import org.example.kalkulationsprogramm.domain.BwaUpload
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BwaUploadRepository : JpaRepository<BwaUpload, Long> {
    fun findByJahrOrderByMonatDesc(jahr: Int?): List<BwaUpload>

    fun findByJahrAndTypOrderByMonatDesc(jahr: Int?, typ: BwaTyp): List<BwaUpload>

    fun findByJahrAndMonat(jahr: Int?, monat: Int?): Optional<BwaUpload>

    fun findByFreigegebenTrueOrderByJahrDescMonatDesc(): List<BwaUpload>

    fun findByJahrAndFreigegebenTrue(jahr: Int?): List<BwaUpload>

    fun existsByJahrAndMonat(jahr: Int?, monat: Int?): Boolean

    fun findByFreigegebenFalseAndAnalysiertTrueOrderByJahrDescMonatDesc(): List<BwaUpload>

    @Query("SELECT COALESCE(SUM(b.kostenAusBwa), 0) FROM BwaUpload b WHERE b.jahr = :jahr AND b.freigegeben = true")
    fun summeGemeinkostenAusBwa(@Param("jahr") jahr: Int?): BigDecimal

    fun existsBySourceEmailIdAndOriginalDateiname(emailId: Long?, originalDateiname: String): Boolean
}
