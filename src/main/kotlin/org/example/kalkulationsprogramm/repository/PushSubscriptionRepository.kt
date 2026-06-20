package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.PushSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PushSubscriptionRepository : JpaRepository<PushSubscription, Long> {
    fun findByMitarbeiterId(mitarbeiterId: Long?): List<PushSubscription>

    fun findByEndpoint(endpoint: String): Optional<PushSubscription>

    fun deleteByEndpoint(endpoint: String)

    fun deleteByMitarbeiterId(mitarbeiterId: Long?)
}
