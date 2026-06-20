package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.EmailBlacklistEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmailBlacklistRepository : JpaRepository<EmailBlacklistEntry, Long> {
    fun existsByEmailAddress(emailAddress: String): Boolean

    fun findByEmailAddress(emailAddress: String): Optional<EmailBlacklistEntry>
}
