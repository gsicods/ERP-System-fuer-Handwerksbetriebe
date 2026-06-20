package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.SpamTokenCount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SpamTokenCountRepository : JpaRepository<SpamTokenCount, Long> {
    fun findByToken(token: String): Optional<SpamTokenCount>

    fun findByTokenIn(tokens: Collection<String>): List<SpamTokenCount>

    @Modifying
    @Query(
        value = "INSERT INTO spam_token_count (token, spam_count, ham_count) VALUES (:token, :spamInc, :hamInc) " +
            "ON DUPLICATE KEY UPDATE spam_count = spam_count + :spamInc, ham_count = ham_count + :hamInc",
        nativeQuery = true,
    )
    fun upsertToken(
        @Param("token") token: String,
        @Param("spamInc") spamIncrement: Int,
        @Param("hamInc") hamIncrement: Int,
    )

    @Modifying
    @Query(
        value = "UPDATE spam_token_count SET " +
            "spam_count = GREATEST(0, spam_count - :spamDec), " +
            "ham_count  = GREATEST(0, ham_count  - :hamDec) " +
            "WHERE token = :token",
        nativeQuery = true,
    )
    fun decrementToken(
        @Param("token") token: String,
        @Param("spamDec") spamDecrement: Int,
        @Param("hamDec") hamDecrement: Int,
    )
}
