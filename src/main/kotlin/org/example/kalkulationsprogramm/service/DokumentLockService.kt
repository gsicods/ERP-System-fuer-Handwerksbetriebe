package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.DokumentLock
import org.example.kalkulationsprogramm.dto.DokumentLockDto
import org.example.kalkulationsprogramm.repository.DokumentLockRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

@Service
class DokumentLockService(
    private val repository: DokumentLockRepository
) {
    @Transactional
    fun acquire(dokumentTyp: String, dokumentId: Long, userId: Long, userDisplayName: String?): DokumentLockDto {
        validateTyp(dokumentTyp)
        val now = LocalDateTime.now()
        val existing = repository.findByDokumentTypAndDokumentId(dokumentTyp, dokumentId)
        if (existing.isPresent) {
            val lock = existing.get()
            val sameUser = lock.userId == userId
            val stale = isStale(lock, now)
            if (sameUser || stale) {
                lock.userId = userId
                lock.userDisplayName = safeDisplayName(userDisplayName)
                if (!sameUser) lock.acquiredAt = now
                lock.lastHeartbeatAt = now
                return acquired(repository.save(lock))
            }
            return lockedByOther(lock)
        }

        val fresh = DokumentLock()
        fresh.dokumentTyp = dokumentTyp
        fresh.dokumentId = dokumentId
        fresh.userId = userId
        fresh.userDisplayName = safeDisplayName(userDisplayName)
        fresh.acquiredAt = now
        fresh.lastHeartbeatAt = now
        return try {
            acquired(repository.saveAndFlush(fresh))
        } catch (race: DataIntegrityViolationException) {
            val winner = repository.findByDokumentTypAndDokumentId(dokumentTyp, dokumentId).orElseThrow { race }
            if (winner.userId == userId) acquired(winner) else lockedByOther(winner)
        }
    }

    @Transactional
    fun heartbeat(dokumentTyp: String, dokumentId: Long, userId: Long, userDisplayName: String?): DokumentLockDto {
        validateTyp(dokumentTyp)
        val existing = repository.findByDokumentTypAndDokumentId(dokumentTyp, dokumentId)
        if (existing.isEmpty) return acquire(dokumentTyp, dokumentId, userId, userDisplayName)
        val lock = existing.get()
        if (lock.userId != userId) {
            return if (isStale(lock, LocalDateTime.now())) acquire(dokumentTyp, dokumentId, userId, userDisplayName) else lockedByOther(lock)
        }
        lock.lastHeartbeatAt = LocalDateTime.now()
        if (!userDisplayName.isNullOrBlank()) lock.userDisplayName = userDisplayName
        return acquired(repository.save(lock))
    }

    @Transactional
    fun release(dokumentTyp: String, dokumentId: Long, userId: Long) {
        validateTyp(dokumentTyp)
        repository.findByDokumentTypAndDokumentId(dokumentTyp, dokumentId)
            .filter { it.userId == userId }
            .ifPresent(repository::delete)
    }

    @Transactional(readOnly = true)
    fun isHeldBy(dokumentTyp: String, dokumentId: Long, userId: Long): Boolean {
        validateTyp(dokumentTyp)
        return repository.findByDokumentTypAndDokumentId(dokumentTyp, dokumentId)
            .map { it.userId == userId && !isStale(it, LocalDateTime.now()) }
            .orElse(false)
    }

    private fun isStale(lock: DokumentLock, now: LocalDateTime): Boolean =
        Duration.between(lock.lastHeartbeatAt, now) > STALE_AFTER

    private fun validateTyp(dokumentTyp: String?) {
        require(dokumentTyp != null && ERLAUBTE_TYPEN.contains(dokumentTyp)) { "Unbekannter dokumentTyp: $dokumentTyp" }
    }

    private fun safeDisplayName(userDisplayName: String?): String {
        if (userDisplayName.isNullOrBlank()) return "Unbekannter Benutzer"
        return if (userDisplayName.length > 255) userDisplayName.substring(0, 255) else userDisplayName
    }

    private fun acquired(lock: DokumentLock): DokumentLockDto =
        DokumentLockDto(DokumentLockDto.ACQUIRED, lock.userId, lock.userDisplayName, lock.acquiredAt, lock.lastHeartbeatAt)

    private fun lockedByOther(lock: DokumentLock): DokumentLockDto =
        DokumentLockDto(DokumentLockDto.LOCKED_BY_OTHER, lock.userId, lock.userDisplayName, lock.acquiredAt, lock.lastHeartbeatAt)

    companion object {
        const val TYP_AUSGANG = "AUSGANG"
        const val TYP_EINGANG = "EINGANG"
        private val ERLAUBTE_TYPEN = setOf(TYP_AUSGANG, TYP_EINGANG)
        @JvmField
        val STALE_AFTER: Duration = Duration.ofSeconds(90)
    }
}
