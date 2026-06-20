package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.EntityLastAccessed
import org.example.kalkulationsprogramm.domain.EntityLastAccessed.EntityLastAccessedId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface EntityLastAccessedRepository : JpaRepository<EntityLastAccessed, EntityLastAccessedId> {
    @Query(
        "SELECT e FROM EntityLastAccessed e " +
            "WHERE e.id.userId = :userId AND e.id.entityType = :entityType " +
            "ORDER BY e.zugegriffenAm DESC",
    )
    fun findAllByUserAndType(
        @Param("userId") userId: Long?,
        @Param("entityType") entityType: String,
    ): List<EntityLastAccessed>
}
