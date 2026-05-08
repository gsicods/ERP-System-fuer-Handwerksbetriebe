package org.example.kalkulationsprogramm.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.example.kalkulationsprogramm.domain.SvSatz;
import org.example.kalkulationsprogramm.domain.SvSatzTyp;
import org.example.kalkulationsprogramm.dto.SvSatzDto;
import org.example.kalkulationsprogramm.repository.SvSatzRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests fuer SvSatzService.
 *
 *  - Validierung (Pflichtfelder, unbekanntes Enum)
 *  - Mapping von DataIntegrityViolation auf sprechende IllegalArgumentException
 *    (Unique-Constraint typ + gueltig_ab)
 */
class SvSatzServiceTest {

    private SvSatzRepository repository;
    private SvSatzService service;

    @BeforeEach
    void setUp() {
        repository = mock(SvSatzRepository.class);
        service = new SvSatzService(repository);
    }

    @Test
    void unbekannterSatzTypWirdAbgewiesen() {
        SvSatzDto dto = new SvSatzDto();
        dto.setSatzTyp("ERFUNDEN_XY");
        dto.setProzent(new BigDecimal("14.6"));
        dto.setGueltigAb(LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ERFUNDEN_XY");
    }

    @Test
    void leererTypWirdAbgewiesen() {
        SvSatzDto dto = new SvSatzDto();
        dto.setProzent(new BigDecimal("14.6"));
        dto.setGueltigAb(LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Typ");
    }

    @Test
    void fehlendesGueltigAbWirdAbgewiesen() {
        SvSatzDto dto = new SvSatzDto();
        dto.setSatzTyp("KV_GESAMT");
        dto.setProzent(new BigDecimal("14.6"));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Gueltig");
    }

    @Test
    void doppelterTypAmGleichenDatumWirftSprechendeMeldung() {
        when(repository.saveAndFlush(any(SvSatz.class)))
                .thenThrow(new DataIntegrityViolationException("uk_sv_satz_typ_ab"));

        SvSatzDto dto = new SvSatzDto();
        dto.setSatzTyp("KV_GESAMT");
        dto.setProzent(new BigDecimal("14.6"));
        dto.setGueltigAb(LocalDate.of(2026, 1, 1));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("KV_GESAMT")
                .hasMessageContaining("2026-01-01");
    }

    @Test
    void neuerSatzWirdGespeichert() {
        when(repository.saveAndFlush(any(SvSatz.class))).thenAnswer(inv -> {
            SvSatz s = inv.getArgument(0);
            s.setId(11L);
            return s;
        });

        SvSatzDto dto = new SvSatzDto();
        dto.setSatzTyp("PV_GESAMT");
        dto.setProzent(new BigDecimal("3.40"));
        dto.setGueltigAb(LocalDate.of(2026, 1, 1));

        SvSatzDto saved = service.save(dto);

        assertThat(saved.getId()).isEqualTo(11L);
        assertThat(saved.getSatzTyp()).isEqualTo("PV_GESAMT");
        assertThat(saved.getProzent()).isEqualByComparingTo("3.40");
    }

    @Test
    void findAllListetSortiertAuf() {
        SvSatz s = new SvSatz();
        s.setId(1L);
        s.setSatzTyp(SvSatzTyp.KV_GESAMT);
        s.setProzent(new BigDecimal("14.60"));
        s.setGueltigAb(LocalDate.of(2026, 1, 1));
        when(repository.findAllByOrderBySatzTypAscGueltigAbDesc()).thenReturn(java.util.List.of(s));

        assertThat(service.findAll()).hasSize(1);
    }
}
