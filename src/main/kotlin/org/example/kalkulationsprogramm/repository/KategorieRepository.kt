package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.Kategorie
import org.springframework.data.jpa.repository.JpaRepository

interface KategorieRepository : JpaRepository<Kategorie, Int> {
    fun findByParentKategorieIsNull(): List<Kategorie>

    fun findByParentKategorie_Id(parentId: Int?): List<Kategorie>

    fun existsByParentKategorie_Id(parentId: Int?): Boolean
}
