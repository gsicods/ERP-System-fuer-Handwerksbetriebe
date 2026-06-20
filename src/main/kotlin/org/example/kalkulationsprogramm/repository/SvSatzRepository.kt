package org.example.kalkulationsprogramm.repository

import java.time.LocalDate
import java.util.Optional
import org.example.kalkulationsprogramm.domain.SvSatz
import org.example.kalkulationsprogramm.domain.SvSatzTyp
import org.springframework.data.jpa.repository.JpaRepository

interface SvSatzRepository : JpaRepository<SvSatz, Long> {
    fun findAllByOrderBySatzTypAscGueltigAbDesc(): List<SvSatz>

    fun findFirstBySatzTypAndGueltigAbLessThanEqualOrderByGueltigAbDesc(
        typ: SvSatzTyp,
        stichtag: LocalDate,
    ): Optional<SvSatz>
}
