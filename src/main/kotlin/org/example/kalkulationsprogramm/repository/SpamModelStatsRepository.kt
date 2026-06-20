package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.SpamModelStats
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SpamModelStatsRepository : JpaRepository<SpamModelStats, Long> {
    fun findByStatKey(statKey: String): Optional<SpamModelStats>

    @Modifying
    @Query("UPDATE SpamModelStats s SET s.statValue = s.statValue + 1 WHERE s.statKey = :key")
    fun incrementStat(@Param("key") key: String): Int

    @Modifying
    @Query("UPDATE SpamModelStats s SET s.statValue = CASE WHEN s.statValue > 0 THEN s.statValue - 1 ELSE 0 END WHERE s.statKey = :key")
    fun decrementStat(@Param("key") key: String): Int
}
