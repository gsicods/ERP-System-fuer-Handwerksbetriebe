package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.Email
import org.example.kalkulationsprogramm.repository.EmailRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class EmailOutboundPersistenceService(
    private val emailRepository: EmailRepository
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = [Exception::class])
    fun speichereOutEmail(email: Email) {
        emailRepository.save(email)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true, noRollbackFor = [Exception::class])
    fun existsByMessageId(messageId: String): Boolean = emailRepository.existsByMessageId(messageId)
}
