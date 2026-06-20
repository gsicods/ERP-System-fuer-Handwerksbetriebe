package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.Email
import org.example.kalkulationsprogramm.domain.EmailAttachment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface EmailAttachmentRepository : JpaRepository<EmailAttachment, Long> {
    fun findByEmail(email: Email): List<EmailAttachment>

    fun findByEmailId(emailId: Long?): List<EmailAttachment>

    @Query("SELECT a FROM EmailAttachment a WHERE a.aiProcessed = false")
    fun findUnprocessed(): List<EmailAttachment>

    @Query(
        """
      SELECT a FROM EmailAttachment a
      WHERE a.aiProcessed = false
        AND LOWER(a.originalFilename) LIKE '%.pdf'
      """,
    )
    fun findUnprocessedDocuments(): List<EmailAttachment>

    fun countByAiProcessedFalse(): Long

    fun findByLieferantDokumentId(lieferantDokumentId: Long?): List<EmailAttachment>
}
