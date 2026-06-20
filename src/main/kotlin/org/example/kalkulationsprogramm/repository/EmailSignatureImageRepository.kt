package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.EmailSignatureImage
import org.springframework.data.jpa.repository.JpaRepository

interface EmailSignatureImageRepository : JpaRepository<EmailSignatureImage, Long> {
    fun findBySignatureIdOrderBySortOrderAsc(signatureId: Long?): List<EmailSignatureImage>
}
