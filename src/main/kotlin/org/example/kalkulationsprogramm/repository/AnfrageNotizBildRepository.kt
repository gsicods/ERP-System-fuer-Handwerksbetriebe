package org.example.kalkulationsprogramm.repository

import org.example.kalkulationsprogramm.domain.AnfrageNotizBild
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AnfrageNotizBildRepository : JpaRepository<AnfrageNotizBild, Long> {
    fun findByNotizId(notizId: Long?): List<AnfrageNotizBild>
}
