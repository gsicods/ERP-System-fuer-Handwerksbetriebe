package org.example.kalkulationsprogramm.repository;

import org.example.kalkulationsprogramm.domain.KasseEinstellung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface KasseEinstellungRepository extends JpaRepository<KasseEinstellung, Long> {

    /**
     * Liefert die Singleton-Konfiguration (die V319-Migration legt sie an).
     * Falls aus irgendeinem Grund leer, liefert Optional.empty().
     */
    @Query("SELECT k FROM KasseEinstellung k ORDER BY k.id ASC")
    Optional<KasseEinstellung> findSingleton();
}
