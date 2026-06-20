package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.DokumentnummerCounter
import org.springframework.data.jpa.repository.JpaRepository

interface DokumentnummerCounterRepository : JpaRepository<DokumentnummerCounter, Long> {
    fun findByMonthKey(monthKey: String): Optional<DokumentnummerCounter>
}
