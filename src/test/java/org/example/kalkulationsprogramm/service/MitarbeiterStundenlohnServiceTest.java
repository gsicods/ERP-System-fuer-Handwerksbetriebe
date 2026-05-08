package org.example.kalkulationsprogramm.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.example.kalkulationsprogramm.domain.Mitarbeiter;
import org.example.kalkulationsprogramm.domain.MitarbeiterStundenlohn;
import org.example.kalkulationsprogramm.dto.Mitarbeiter.MitarbeiterStundenlohnDto;
import org.example.kalkulationsprogramm.repository.AbteilungRepository;
import org.example.kalkulationsprogramm.repository.KrankenkasseRepository;
import org.example.kalkulationsprogramm.repository.MitarbeiterDokumentRepository;
import org.example.kalkulationsprogramm.repository.MitarbeiterNotizRepository;
import org.example.kalkulationsprogramm.repository.MitarbeiterRepository;
import org.example.kalkulationsprogramm.repository.MitarbeiterStundenlohnRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests fuer die Stundenlohn-Verlauf-Logik im MitarbeiterService.
 *
 * Kernpunkt: nach jedem Insert/Update/Delete muss mitarbeiter.stundenlohn auf
 * den aktuell gueltigen Eintrag (gueltig_ab &lt;= heute, juengster) gespiegelt
 * werden - ansonsten verliert der Code aussen die "aktueller Stundenlohn"-Sicht.
 */
class MitarbeiterStundenlohnServiceTest {

    private MitarbeiterRepository mitarbeiterRepository;
    private MitarbeiterStundenlohnRepository stundenlohnRepository;
    private MitarbeiterService service;
    private Mitarbeiter mitarbeiter;

    @BeforeEach
    void setUp() {
        mitarbeiterRepository = mock(MitarbeiterRepository.class);
        stundenlohnRepository = mock(MitarbeiterStundenlohnRepository.class);
        service = new MitarbeiterService(
                mitarbeiterRepository,
                mock(MitarbeiterDokumentRepository.class),
                mock(MitarbeiterNotizRepository.class),
                mock(AbteilungRepository.class),
                mock(KrankenkasseRepository.class),
                stundenlohnRepository
        );
        mitarbeiter = new Mitarbeiter();
        mitarbeiter.setId(1L);
        mitarbeiter.setVorname("Max");
        mitarbeiter.setNachname("Mustermann");
        when(mitarbeiterRepository.findById(1L)).thenReturn(Optional.of(mitarbeiter));
        when(mitarbeiterRepository.save(any(Mitarbeiter.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stundenlohnRepository.save(any(MitarbeiterStundenlohn.class))).thenAnswer(inv -> {
            MitarbeiterStundenlohn s = inv.getArgument(0);
            if (s.getId() == null) s.setId(100L);
            return s;
        });
    }

    @Test
    void negativerStundenlohnWirdAbgewiesen() {
        MitarbeiterStundenlohnDto dto = new MitarbeiterStundenlohnDto();
        dto.setStundenlohn(new BigDecimal("-1.00"));
        dto.setGueltigAb(LocalDate.now());

        assertThatThrownBy(() -> service.addStundenlohn(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nicht negativ");
    }

    @Test
    void fehlendesGueltigAbWirdAbgewiesen() {
        MitarbeiterStundenlohnDto dto = new MitarbeiterStundenlohnDto();
        dto.setStundenlohn(new BigDecimal("25.00"));

        assertThatThrownBy(() -> service.addStundenlohn(1L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Gueltig");
    }

    @Test
    void unbekannterMitarbeiterWirftIllegalArgumentException() {
        when(mitarbeiterRepository.findById(99L)).thenReturn(Optional.empty());
        MitarbeiterStundenlohnDto dto = new MitarbeiterStundenlohnDto();
        dto.setStundenlohn(new BigDecimal("25.00"));
        dto.setGueltigAb(LocalDate.now());

        assertThatThrownBy(() -> service.addStundenlohn(99L, dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    @Test
    void addStundenlohnSpiegeltAktuellenWertAufMitarbeiter() {
        // Nach dem Insert: Repo liefert neuen Eintrag als juengsten <= heute zurueck.
        MitarbeiterStundenlohn neu = eintrag(101L, new BigDecimal("28.50"), LocalDate.now());
        when(stundenlohnRepository
                .findFirstByMitarbeiterIdAndGueltigAbLessThanEqualOrderByGueltigAbDesc(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(neu));

        MitarbeiterStundenlohnDto dto = new MitarbeiterStundenlohnDto();
        dto.setStundenlohn(new BigDecimal("28.50"));
        dto.setGueltigAb(LocalDate.now());

        service.addStundenlohn(1L, dto);

        ArgumentCaptor<Mitarbeiter> captor = ArgumentCaptor.forClass(Mitarbeiter.class);
        verify(mitarbeiterRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getStundenlohn()).isEqualByComparingTo("28.50");
    }

    @Test
    void zukuenftigerEintragAendertAktuellenStundenlohnNicht() {
        // Es gibt einen aktuellen Eintrag (gestern, 25 EUR). Heute legt der User
        // einen Eintrag fuer naechsten Monat an (28,50 EUR). Erwartung:
        // mitarbeiter.stundenlohn bleibt 25 EUR, weil der Stichtag heute den
        // alten Eintrag noch greift.
        MitarbeiterStundenlohn alt = eintrag(50L, new BigDecimal("25.00"), LocalDate.now().minusDays(30));
        when(stundenlohnRepository
                .findFirstByMitarbeiterIdAndGueltigAbLessThanEqualOrderByGueltigAbDesc(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(alt));

        MitarbeiterStundenlohnDto dto = new MitarbeiterStundenlohnDto();
        dto.setStundenlohn(new BigDecimal("28.50"));
        dto.setGueltigAb(LocalDate.now().plusDays(30));

        service.addStundenlohn(1L, dto);

        ArgumentCaptor<Mitarbeiter> captor = ArgumentCaptor.forClass(Mitarbeiter.class);
        verify(mitarbeiterRepository).save(captor.capture());
        assertThat(captor.getValue().getStundenlohn()).isEqualByComparingTo("25.00");
    }

    @Test
    void deleteStundenlohnRefreshtMitarbeiterStundenlohn() {
        MitarbeiterStundenlohn loeschKandidat = eintrag(77L, new BigDecimal("28.50"), LocalDate.now());
        loeschKandidat.setMitarbeiter(mitarbeiter);
        when(stundenlohnRepository.findById(77L)).thenReturn(Optional.of(loeschKandidat));
        // Nach dem Delete: Repo liefert den naechstaelteren Eintrag.
        MitarbeiterStundenlohn fallback = eintrag(50L, new BigDecimal("22.00"), LocalDate.now().minusDays(30));
        when(stundenlohnRepository
                .findFirstByMitarbeiterIdAndGueltigAbLessThanEqualOrderByGueltigAbDesc(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(fallback));

        service.deleteStundenlohn(77L);

        verify(stundenlohnRepository).delete(loeschKandidat);
        ArgumentCaptor<Mitarbeiter> captor = ArgumentCaptor.forClass(Mitarbeiter.class);
        verify(mitarbeiterRepository).save(captor.capture());
        assertThat(captor.getValue().getStundenlohn()).isEqualByComparingTo("22.00");
    }

    @Test
    void deleteLetztenEintragSetztStundenlohnAufNull() {
        MitarbeiterStundenlohn einziger = eintrag(77L, new BigDecimal("25.00"), LocalDate.now());
        einziger.setMitarbeiter(mitarbeiter);
        when(stundenlohnRepository.findById(77L)).thenReturn(Optional.of(einziger));
        when(stundenlohnRepository
                .findFirstByMitarbeiterIdAndGueltigAbLessThanEqualOrderByGueltigAbDesc(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        service.deleteStundenlohn(77L);

        ArgumentCaptor<Mitarbeiter> captor = ArgumentCaptor.forClass(Mitarbeiter.class);
        verify(mitarbeiterRepository).save(captor.capture());
        assertThat(captor.getValue().getStundenlohn()).isNull();
    }

    @Test
    void getStundenlohnAmGreiftAufRepositoryStichtagsLogikZurueck() {
        when(stundenlohnRepository
                .findFirstByMitarbeiterIdAndGueltigAbLessThanEqualOrderByGueltigAbDesc(1L, LocalDate.of(2026, 1, 15)))
                .thenReturn(Optional.of(eintrag(11L, new BigDecimal("24.00"), LocalDate.of(2026, 1, 1))));

        BigDecimal lohn = service.getStundenlohnAm(1L, LocalDate.of(2026, 1, 15));

        assertThat(lohn).isEqualByComparingTo("24.00");
    }

    @Test
    void getStundenlohnAmOhneEintragLiefertNull() {
        when(stundenlohnRepository
                .findFirstByMitarbeiterIdAndGueltigAbLessThanEqualOrderByGueltigAbDesc(eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        assertThat(service.getStundenlohnAm(1L, LocalDate.of(2020, 1, 1))).isNull();
    }

    @Test
    void listStundenloehneRuftRepoMitMitarbeiterIdAuf() {
        when(stundenlohnRepository.findByMitarbeiterIdOrderByGueltigAbDesc(1L))
                .thenReturn(List.of(eintrag(10L, new BigDecimal("25.00"), LocalDate.now())));

        List<MitarbeiterStundenlohnDto> list = service.listStundenloehne(1L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getStundenlohn()).isEqualByComparingTo("25.00");
        assertThat(list.get(0).getMitarbeiterId()).isEqualTo(1L);
    }

    private MitarbeiterStundenlohn eintrag(Long id, BigDecimal lohn, LocalDate gueltigAb) {
        MitarbeiterStundenlohn e = new MitarbeiterStundenlohn();
        e.setId(id);
        e.setStundenlohn(lohn);
        e.setGueltigAb(gueltigAb);
        e.setMitarbeiter(mitarbeiter);
        return e;
    }
}
