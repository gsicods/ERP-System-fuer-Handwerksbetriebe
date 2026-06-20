package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.SteuerberaterKontakt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SteuerberaterKontaktRepository : JpaRepository<SteuerberaterKontakt, Long> {
    fun findByAktivTrue(): List<SteuerberaterKontakt>

    fun findByEmailIgnoreCase(email: String): Optional<SteuerberaterKontakt>

    fun findByAktivTrueAndAutoProcessEmailsTrue(): List<SteuerberaterKontakt>

    fun existsByEmailIgnoreCaseAndAktivTrue(email: String): Boolean
}
