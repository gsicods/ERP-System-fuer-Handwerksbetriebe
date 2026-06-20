package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.Kunde
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface KundeRepository : JpaRepository<Kunde, Long>, JpaSpecificationExecutor<Kunde> {
    fun findByKundennummerIgnoreCase(kundennummer: String): Optional<Kunde>

    @Query(value = "SELECT kundennummer FROM kunde ORDER BY CAST(kundennummer AS UNSIGNED) DESC LIMIT 1", nativeQuery = true)
    fun findMaxKundennummer(): Optional<String>

    @Query(
        "SELECT DISTINCT k FROM Kunde k LEFT JOIN k.kundenEmails e " +
            "WHERE LOWER(k.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(k.ansprechspartner) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(k.kundennummer) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(e) LIKE LOWER(CONCAT('%', :query, '%'))",
    )
    fun searchByNameOrAnsprechpartnerOrEmail(@Param("query") query: String): List<Kunde>

    @Query("SELECT COUNT(k) > 0 FROM Kunde k JOIN k.kundenEmails e WHERE LOWER(e) = LOWER(:email)")
    fun existsByKundenEmail(@Param("email") email: String): Boolean

    @Query("SELECT DISTINCT k FROM Kunde k JOIN k.kundenEmails e WHERE LOWER(e) = LOWER(:email)")
    fun findByKundenEmailIgnoreCase(@Param("email") email: String): List<Kunde>

    @Query(
        "SELECT DISTINCT k FROM Kunde k LEFT JOIN k.kundenEmails e WHERE " +
            "(:email IS NOT NULL AND LOWER(e) = :email) OR " +
            "(:telefonDigits IS NOT NULL AND k.telefon IS NOT NULL AND " +
            "  REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(k.telefon, ' ', ''), '-', ''), '/', ''), '(', ''), ')', ''), '.', ''), '+49', '0'), '0049', '0'), '+', '') = :telefonDigits) OR " +
            "(:mobilDigits IS NOT NULL AND k.mobiltelefon IS NOT NULL AND " +
            "  REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(k.mobiltelefon, ' ', ''), '-', ''), '/', ''), '(', ''), ')', ''), '.', ''), '+49', '0'), '0049', '0'), '+', '') = :mobilDigits) OR " +
            "(:name IS NOT NULL AND :plz IS NOT NULL AND LOWER(k.name) = :name AND k.plz = :plz) OR " +
            "(:name IS NOT NULL AND :strasse IS NOT NULL AND LOWER(k.name) = :name AND " +
            "  REPLACE(REPLACE(LOWER(k.strasse), 'straße', 'str.'), 'strasse', 'str.') = :strasse)",
    )
    fun findePotenzielleDuplikate(
        @Param("email") emailLower: String?,
        @Param("telefonDigits") telefonDigits: String?,
        @Param("mobilDigits") mobilDigits: String?,
        @Param("name") nameLower: String?,
        @Param("plz") plz: String?,
        @Param("strasse") strasseLower: String?,
    ): List<Kunde>
}
