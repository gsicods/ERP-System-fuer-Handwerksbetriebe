package org.example.kalkulationsprogramm.repository.miete

import org.example.kalkulationsprogramm.domain.miete.Mietpartei
import org.example.kalkulationsprogramm.domain.miete.Verteilungsschluessel
import org.example.kalkulationsprogramm.domain.miete.VerteilungsschluesselEintrag
import org.springframework.data.jpa.repository.JpaRepository

interface VerteilungsschluesselEintragRepository : JpaRepository<VerteilungsschluesselEintrag, Long> {
    fun findByVerteilungsschluessel(verteilungsschluessel: Verteilungsschluessel): List<VerteilungsschluesselEintrag>

    fun findByMietpartei(mietpartei: Mietpartei): List<VerteilungsschluesselEintrag>
}
