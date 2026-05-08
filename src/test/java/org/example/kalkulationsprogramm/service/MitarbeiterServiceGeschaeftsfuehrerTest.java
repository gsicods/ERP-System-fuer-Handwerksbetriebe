package org.example.kalkulationsprogramm.service;

import java.math.BigDecimal;
import java.util.Optional;

import org.example.kalkulationsprogramm.domain.Mitarbeiter;
import org.example.kalkulationsprogramm.dto.Mitarbeiter.MitarbeiterDto;
import org.example.kalkulationsprogramm.dto.Mitarbeiter.MitarbeiterErstellenDto;
import org.example.kalkulationsprogramm.repository.AbteilungRepository;
import org.example.kalkulationsprogramm.repository.KrankenkasseRepository;
import org.example.kalkulationsprogramm.repository.MitarbeiterDokumentRepository;
import org.example.kalkulationsprogramm.repository.MitarbeiterNotizRepository;
import org.example.kalkulationsprogramm.repository.MitarbeiterRepository;
import org.example.kalkulationsprogramm.repository.MitarbeiterStundenlohnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verrechnungslohn-Rechner Bauabschnitt 1: GF-Felder muessen nur dann
 * persistiert werden, wenn die Person als Geschaeftsfuehrer markiert ist,
 * und sie muessen plausibel sein (kalkulatorischer Lohn Pflicht und nicht
 * negativ; Geldwert-Vorteil optional, aber wenn gesetzt nicht negativ).
 */
class MitarbeiterServiceGeschaeftsfuehrerTest {

    private MitarbeiterRepository mitarbeiterRepository;
    private MitarbeiterService service;

    @BeforeEach
    void setUp() {
        mitarbeiterRepository = mock(MitarbeiterRepository.class);
        service = new MitarbeiterService(
                mitarbeiterRepository,
                mock(MitarbeiterDokumentRepository.class),
                mock(MitarbeiterNotizRepository.class),
                mock(AbteilungRepository.class),
                mock(KrankenkasseRepository.class),
                mock(MitarbeiterStundenlohnRepository.class)
        );
        when(mitarbeiterRepository.save(any(Mitarbeiter.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private MitarbeiterErstellenDto basisDto() {
        MitarbeiterErstellenDto dto = new MitarbeiterErstellenDto();
        dto.setVorname("Max");
        dto.setNachname("Mustermann");
        return dto;
    }

    @Test
    void gfFlagTrueOhneKalkulatorischenLohnWirdAbgewiesen() {
        MitarbeiterErstellenDto dto = basisDto();
        dto.setIstGeschaeftsfuehrer(true);
        // kalkulatorischerLohnMonat fehlt absichtlich

        assertThatThrownBy(() -> service.save(null, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kalkulatorischer Lohn");
    }

    @Test
    void gfFlagTrueMitNegativemLohnWirdAbgewiesen() {
        MitarbeiterErstellenDto dto = basisDto();
        dto.setIstGeschaeftsfuehrer(true);
        dto.setKalkulatorischerLohnMonat(new BigDecimal("-100.00"));

        assertThatThrownBy(() -> service.save(null, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nicht negativ");
    }

    @Test
    void gfFlagTrueMitNegativemGeldwertVorteilWirdAbgewiesen() {
        MitarbeiterErstellenDto dto = basisDto();
        dto.setIstGeschaeftsfuehrer(true);
        dto.setKalkulatorischerLohnMonat(new BigDecimal("5000.00"));
        dto.setGeldwertVorteilMonat(new BigDecimal("-50.00"));

        assertThatThrownBy(() -> service.save(null, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Geldwerte Vorteile");
    }

    @Test
    void gfFlagTrueMitGueltigenWertenPersistiert() {
        MitarbeiterErstellenDto dto = basisDto();
        dto.setIstGeschaeftsfuehrer(true);
        dto.setKalkulatorischerLohnMonat(new BigDecimal("5000.00"));
        dto.setGeldwertVorteilMonat(new BigDecimal("500.00"));

        MitarbeiterDto result = service.save(null, dto);

        assertThat(result.getIstGeschaeftsfuehrer()).isTrue();
        assertThat(result.getKalkulatorischerLohnMonat()).isEqualByComparingTo("5000.00");
        assertThat(result.getGeldwertVorteilMonat()).isEqualByComparingTo("500.00");
    }

    @Test
    void gfFlagFalseSetztKalkLohnUndGeldwertVorteilAufNull() {
        // Bestand: Mitarbeiter ist aktuell GF mit Werten.
        Mitarbeiter bestand = new Mitarbeiter();
        bestand.setId(1L);
        bestand.setIstGeschaeftsfuehrer(true);
        bestand.setKalkulatorischerLohnMonat(new BigDecimal("4000.00"));
        bestand.setGeldwertVorteilMonat(new BigDecimal("300.00"));
        when(mitarbeiterRepository.findById(1L)).thenReturn(Optional.of(bestand));

        MitarbeiterErstellenDto dto = basisDto();
        dto.setIstGeschaeftsfuehrer(false);
        dto.setKalkulatorischerLohnMonat(new BigDecimal("4000.00")); // wird ignoriert
        dto.setGeldwertVorteilMonat(new BigDecimal("300.00"));       // wird ignoriert

        MitarbeiterDto result = service.save(1L, dto);

        assertThat(result.getIstGeschaeftsfuehrer()).isFalse();
        assertThat(result.getKalkulatorischerLohnMonat()).isNull();
        assertThat(result.getGeldwertVorteilMonat()).isNull();
    }

    @Test
    void gfFlagNullDefaultsAufFalseUndKeineValidierung() {
        MitarbeiterErstellenDto dto = basisDto();
        // istGeschaeftsfuehrer = null → wie false, keine Validierung
        dto.setKalkulatorischerLohnMonat(null);

        MitarbeiterDto result = service.save(null, dto);

        assertThat(result.getIstGeschaeftsfuehrer()).isFalse();
        assertThat(result.getKalkulatorischerLohnMonat()).isNull();
    }
}
