package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.Produktkategorie
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ProduktkategorieRepository : JpaRepository<Produktkategorie, Long> {
    @EntityGraph(attributePaths = ["unterkategorien"])
    fun findByUebergeordneteKategorieIsNull(): List<Produktkategorie>

    @EntityGraph(attributePaths = ["unterkategorien"])
    fun findByUebergeordneteKategorieId(parentId: Long?): List<Produktkategorie>

    @Query("SELECT k FROM Produktkategorie k LEFT JOIN FETCH k.uebergeordneteKategorie")
    fun findAllWithParent(): List<Produktkategorie>

    @Query("SELECT k.uebergeordneteKategorie.id FROM Produktkategorie k WHERE k.uebergeordneteKategorie IS NOT NULL")
    fun findAllParentIds(): List<Long>

    @Query("SELECT pk FROM Produktkategorie pk WHERE pk.unterkategorien IS EMPTY AND LOWER(pk.bezeichnung) LIKE LOWER(CONCAT('%', :suchbegriff, '%'))")
    fun sucheLeafKategorienNachBezeichnung(@Param("suchbegriff") suchbegriff: String): List<Produktkategorie>
}
