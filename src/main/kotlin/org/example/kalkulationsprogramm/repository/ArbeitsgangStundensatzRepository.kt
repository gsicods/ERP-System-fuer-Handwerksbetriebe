package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.ArbeitsgangStundensatz
import org.springframework.data.jpa.repository.JpaRepository

interface ArbeitsgangStundensatzRepository : JpaRepository<ArbeitsgangStundensatz, Long> {
    fun findTopByArbeitsgangIdOrderByJahrDesc(arbeitsgangId: Long?): Optional<ArbeitsgangStundensatz>

    fun findTopByArbeitsgangIdAndJahrOrderByIdDesc(arbeitsgangId: Long?, jahr: Int): Optional<ArbeitsgangStundensatz>

    fun findTopByArbeitsgangIdAndJahrGreaterThanEqualOrderByJahrAsc(
        arbeitsgangId: Long?,
        jahr: Int,
    ): Optional<ArbeitsgangStundensatz>
}
