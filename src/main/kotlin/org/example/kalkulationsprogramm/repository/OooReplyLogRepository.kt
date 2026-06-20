package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.OooReplyLog
import org.springframework.data.jpa.repository.JpaRepository

interface OooReplyLogRepository : JpaRepository<OooReplyLog, Long> {
    fun existsByScheduleIdAndSenderAddressIgnoreCase(scheduleId: Long?, senderAddress: String): Boolean
}
