package org.example.kalkulationsprogramm.repository

import jakarta.persistence.LockModeType
import java.util.Optional
import org.example.kalkulationsprogramm.domain.Mitarbeiter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MitarbeiterRepository : JpaRepository<Mitarbeiter, Long> {
    fun findByLoginToken(loginToken: String): Optional<Mitarbeiter>

    fun findByLoginTokenAndAktivTrue(loginToken: String): Optional<Mitarbeiter>

    fun findByAktivTrue(): List<Mitarbeiter>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Mitarbeiter m WHERE m.loginToken = :token AND m.aktiv = true")
    fun findByLoginTokenAndAktivTrueForUpdate(@Param("token") token: String): Optional<Mitarbeiter>
}
