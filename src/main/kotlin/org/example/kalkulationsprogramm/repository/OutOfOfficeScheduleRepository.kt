package org.example.kalkulationsprogramm.repository

import java.time.LocalDate
import java.util.Optional
import org.example.kalkulationsprogramm.domain.OutOfOfficeSchedule
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface OutOfOfficeScheduleRepository : JpaRepository<OutOfOfficeSchedule, Long> {
    fun findByActiveTrueAndStartAtLessThanEqualAndEndAtGreaterThanEqual(
        start: LocalDate,
        end: LocalDate,
    ): List<OutOfOfficeSchedule>

    @EntityGraph(attributePaths = ["signature"])
    fun findFirstByActiveTrueAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByStartAtDesc(
        start: LocalDate,
        end: LocalDate,
    ): Optional<OutOfOfficeSchedule>
}
