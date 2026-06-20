package org.example.kalkulationsprogramm.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
open class DokumentLock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @Column(name = "dokument_typ", nullable = false, length = 16)
    open var dokumentTyp: String? = null

    @Column(name = "dokument_id", nullable = false)
    open var dokumentId: Long? = null

    @Column(name = "user_id", nullable = false)
    open var userId: Long? = null

    @Column(name = "user_display_name", nullable = false, length = 255)
    open var userDisplayName: String? = null

    @Column(name = "acquired_at", nullable = false)
    open var acquiredAt: LocalDateTime? = null

    @Column(name = "last_heartbeat_at", nullable = false)
    open var lastHeartbeatAt: LocalDateTime? = null

}
