package org.example.kalkulationsprogramm.dto

import java.time.LocalDateTime

data class DokumentLockDto(
    val status: String,
    val holderUserId: Long?,
    val holderDisplayName: String?,
    val acquiredAt: LocalDateTime?,
    val lastHeartbeatAt: LocalDateTime?
) {
    fun status(): String = status
    fun holderUserId(): Long? = holderUserId
    fun holderDisplayName(): String? = holderDisplayName
    fun acquiredAt(): LocalDateTime? = acquiredAt
    fun lastHeartbeatAt(): LocalDateTime? = lastHeartbeatAt

    companion object {
        const val ACQUIRED = "ACQUIRED"
        const val LOCKED_BY_OTHER = "LOCKED_BY_OTHER"
    }
}
