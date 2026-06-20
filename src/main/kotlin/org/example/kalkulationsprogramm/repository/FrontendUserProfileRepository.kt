package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.FrontendUserProfile
import org.example.kalkulationsprogramm.domain.FrontendUserRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface FrontendUserProfileRepository : JpaRepository<FrontendUserProfile, Long> {
    fun findByDisplayNameIgnoreCase(displayName: String): Optional<FrontendUserProfile>

    fun findByUsernameIgnoreCase(username: String): Optional<FrontendUserProfile>

    fun findByMitarbeiterIdAndActiveTrue(mitarbeiterId: Long?): Optional<FrontendUserProfile>

    fun existsByUsernameIgnoreCase(username: String): Boolean

    fun countByUsernameIsNotNull(): Long

    @Query(
        "SELECT COUNT(p) FROM FrontendUserProfile p " +
            "JOIN p.roleSet r " +
            "WHERE p.active = true AND r = :role AND p.id <> :excludedId",
    )
    fun countActiveByRoleExcludingId(
        @Param("role") role: FrontendUserRole,
        @Param("excludedId") excludedId: Long?,
    ): Long
}
