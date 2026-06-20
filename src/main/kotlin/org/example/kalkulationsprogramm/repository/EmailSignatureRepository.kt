package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.EmailSignature
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface EmailSignatureRepository : JpaRepository<EmailSignature, Long> {
    fun findAllByOrderByUpdatedAtDesc(): List<EmailSignature>

    fun findFirstByIsSystemDefaultTrue(): Optional<EmailSignature>

    @Modifying
    @Query("UPDATE EmailSignature s SET s.isSystemDefault = false WHERE s.isSystemDefault = true AND s.id <> :keepId")
    fun clearSystemDefaultExcept(@Param("keepId") keepId: Long?): Int
}
