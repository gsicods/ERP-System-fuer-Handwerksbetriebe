package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.AenderungsgrundKatalog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AenderungsgrundKatalogRepository : JpaRepository<AenderungsgrundKatalog, Long> {
    fun findByCode(code: String): Optional<AenderungsgrundKatalog>

    fun findAllByOrderByBezeichnungAsc(): List<AenderungsgrundKatalog>
}
