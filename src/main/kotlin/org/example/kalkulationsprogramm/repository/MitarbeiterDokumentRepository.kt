package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.MitarbeiterDokument
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MitarbeiterDokumentRepository : JpaRepository<MitarbeiterDokument, Long> {
    fun findByMitarbeiterId(mitarbeiterId: Long?): List<MitarbeiterDokument>

    fun findByGespeicherterDateiname(gespeicherterDateiname: String): Optional<MitarbeiterDokument>
}
