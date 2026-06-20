package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.EmailTextTemplate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmailTextTemplateRepository : JpaRepository<EmailTextTemplate, Long> {
    fun findByDokumentTyp(dokumentTyp: String): Optional<EmailTextTemplate>
}
