package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.EmailDraft
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface EmailDraftRepository : JpaRepository<EmailDraft, Long> {
    @Query("SELECT d FROM EmailDraft d ORDER BY d.updatedAt DESC")
    fun findAllByOrderByUpdatedAtDesc(): List<EmailDraft>

    fun findByReplyEmailIdIn(emailIds: Collection<Long>): List<EmailDraft>
}
