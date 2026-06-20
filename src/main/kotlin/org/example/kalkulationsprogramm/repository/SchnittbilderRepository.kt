package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.Schnittbilder
import org.springframework.data.jpa.repository.JpaRepository

interface SchnittbilderRepository : JpaRepository<Schnittbilder, Long> {
    fun findByKategorie_Id(kategorieId: Int?): List<Schnittbilder>

    fun findByForm(form: String): Schnittbilder
}
