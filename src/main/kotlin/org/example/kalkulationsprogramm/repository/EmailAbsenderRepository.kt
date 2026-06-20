package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.EmailAbsender
import org.springframework.data.jpa.repository.JpaRepository

interface EmailAbsenderRepository : JpaRepository<EmailAbsender, Long> {
    fun findAllByOrderBySortierungAscIdAsc(): List<EmailAbsender>

    fun findByAktivTrueOrderBySortierungAscIdAsc(): List<EmailAbsender>

    fun findFirstByAktivTrueOrderBySortierungAscIdAsc(): Optional<EmailAbsender>

    fun findByEmailAdresseIgnoreCase(emailAdresse: String): Optional<EmailAbsender>
}
