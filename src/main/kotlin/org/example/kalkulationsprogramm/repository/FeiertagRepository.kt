package org.example.kalkulationsprogramm.repository

import java.time.LocalDate
import java.util.Optional
import org.example.kalkulationsprogramm.domain.Feiertag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface FeiertagRepository : JpaRepository<Feiertag, Long> {
    fun findByBundesland(bundesland: String): List<Feiertag>

    @Query("SELECT f FROM Feiertag f WHERE YEAR(f.datum) = :jahr AND f.bundesland = :bundesland ORDER BY f.datum")
    fun findByJahrAndBundesland(jahr: Int, bundesland: String): List<Feiertag>

    @Query("SELECT f FROM Feiertag f WHERE YEAR(f.datum) = :jahr ORDER BY f.datum")
    fun findByJahr(jahr: Int): List<Feiertag>

    fun existsByDatumAndBundesland(datum: LocalDate, bundesland: String): Boolean

    @Query("SELECT f FROM Feiertag f WHERE f.datum BETWEEN :von AND :bis ORDER BY f.datum")
    fun findByDatumBetween(von: LocalDate, bis: LocalDate): List<Feiertag>

    fun findByDatumAndBundesland(datum: LocalDate, bundesland: String): Optional<Feiertag>
}
