package org.example.kalkulationsprogramm.repository;

import org.example.kalkulationsprogramm.domain.Beleg;
import org.example.kalkulationsprogramm.domain.BelegKategorie;
import org.example.kalkulationsprogramm.domain.BelegStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface BelegRepository extends JpaRepository<Beleg, Long> {

    List<Beleg> findByStatusOrderByUploadDatumDesc(BelegStatus status);

    List<Beleg> findByStatusAndBelegKategorieOrderByBelegDatumDesc(BelegStatus status, BelegKategorie kategorie);

    List<Beleg> findAllByOrderByUploadDatumDesc();

    @Query("SELECT b FROM Beleg b WHERE b.status = :status AND b.belegKategorie IN :kategorien ORDER BY b.belegDatum DESC, b.uploadDatum DESC")
    List<Beleg> findValidierteByKategorien(@Param("status") BelegStatus status,
                                           @Param("kategorien") List<BelegKategorie> kategorien);

    /**
     * Summe brutto über alle validierten Belege einer Kategorie, optional gefiltert
     * nach Belegdatum. Wird für den Kassen-Saldo verwendet.
     */
    @Query("SELECT COALESCE(SUM(b.betragBrutto), 0) FROM Beleg b " +
           "WHERE b.status = org.example.kalkulationsprogramm.domain.BelegStatus.VALIDIERT " +
           "  AND b.belegKategorie = :kategorie " +
           "  AND (:von IS NULL OR b.belegDatum >= :von) " +
           "  AND (:bis IS NULL OR b.belegDatum <= :bis)")
    BigDecimal summeBruttoByKategorie(@Param("kategorie") BelegKategorie kategorie,
                                      @Param("von") LocalDate von,
                                      @Param("bis") LocalDate bis);

    long countByKiAnalyseStatus(org.example.kalkulationsprogramm.domain.BelegKiAnalyseStatus status);
}
