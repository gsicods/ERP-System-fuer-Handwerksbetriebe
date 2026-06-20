package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.Lieferanten
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LieferantenRepository : JpaRepository<Lieferanten, Long>, JpaSpecificationExecutor<Lieferanten> {
    fun findByLieferantenname(lieferantenname: String): Optional<Lieferanten>

    fun findByLieferantennameIgnoreCase(lieferantenname: String): Optional<Lieferanten>

    @Query("select l from Lieferanten l join l.kundenEmails e where e = :email")
    fun findByEmail(@Param("email") email: String): Optional<Lieferanten>

    @Query("select distinct l from Lieferanten l join l.kundenEmails e where lower(e) like concat('%@', lower(:domain))")
    fun findByEmailDomain(@Param("domain") domain: String): List<Lieferanten>

    @Query("select case when count(l) > 0 then true else false end from Lieferanten l join l.kundenEmails e where lower(e) like concat('%@', lower(:domain))")
    fun existsByEmailDomain(@Param("domain") domain: String): Boolean

    @Query("select distinct l from Lieferanten l join l.artikelpreise ap join ap.artikel a")
    fun findAllWithArtikel(): List<Lieferanten>

    @EntityGraph(attributePaths = ["kundenEmails"])
    @Query("select l from Lieferanten l")
    fun findAllWithEmails(): List<Lieferanten>

    fun findByIstAktivTrueOrderByLieferantennameAsc(): List<Lieferanten>

    @Query("select distinct l from Lieferanten l left join l.kundenEmails e where lower(l.lieferantenname) like lower(concat('%', :query, '%')) or lower(e) like lower(concat('%', :query, '%'))")
    fun searchByNameOrEmail(@Param("query") query: String): List<Lieferanten>
}
