package org.example.kalkulationsprogramm.service

import org.example.kalkulationsprogramm.domain.EntityLastAccessed
import org.example.kalkulationsprogramm.repository.EntityLastAccessedRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.LinkedHashMap

@Service
class EntityLastAccessedService(
    private val repository: EntityLastAccessedRepository
) {

    @Transactional
    fun track(userId: Long?, entityType: String?, entityId: Long?) {
        if (userId == null || entityType == null || entityId == null) {
            return
        }
        val entry = EntityLastAccessed(userId, entityType, entityId, LocalDateTime.now())
        repository.save(entry)
    }

    @Transactional(readOnly = true)
    fun listForUser(userId: Long?, entityType: String?): Map<Long, Long> {
        if (userId == null || entityType == null) {
            return emptyMap()
        }
        val entries = repository.findAllByUserAndType(userId, entityType)
        val result = LinkedHashMap<Long, Long>(entries.size)
        for (entry in entries) {
            val timestamp = entry.zugegriffenAm ?: continue
            val epochMillis = timestamp.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val entityId = entry.id?.entityId ?: continue
            result[entityId] = epochMillis
        }
        return result
    }
}
