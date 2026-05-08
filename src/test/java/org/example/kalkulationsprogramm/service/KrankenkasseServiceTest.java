package org.example.kalkulationsprogramm.service;

import java.math.BigDecimal;
import java.util.List;

import org.example.kalkulationsprogramm.domain.Krankenkasse;
import org.example.kalkulationsprogramm.dto.KrankenkasseDto;
import org.example.kalkulationsprogramm.repository.KrankenkasseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests fuer KrankenkasseService.
 *
 * Validierungs-Pfade:
 *  - leerer Name
 *  - fehlender Zusatzbeitrag
 * Save (insert + update), Liste, Loeschen.
 */
class KrankenkasseServiceTest {

    private KrankenkasseRepository repository;
    private KrankenkasseService service;

    @BeforeEach
    void setUp() {
        repository = mock(KrankenkasseRepository.class);
        service = new KrankenkasseService(repository);
        when(repository.save(any(Krankenkasse.class))).thenAnswer(inv -> {
            Krankenkasse e = inv.getArgument(0);
            if (e.getId() == null) {
                e.setId(42L);
            }
            return e;
        });
    }

    @Test
    void leererNameWirdAbgewiesen() {
        KrankenkasseDto dto = new KrankenkasseDto();
        dto.setName("   ");
        dto.setZusatzbeitragProzent(new BigDecimal("2.5"));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Name");
    }

    @Test
    void fehlenderZusatzbeitragWirdAbgewiesen() {
        KrankenkasseDto dto = new KrankenkasseDto();
        dto.setName("Test-KK");

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Zusatzbeitrag");
    }

    @Test
    void neueKrankenkasseWirdGespeichert() {
        KrankenkasseDto dto = new KrankenkasseDto();
        dto.setName("Test-KK");
        dto.setKuerzel("TKK");
        dto.setZusatzbeitragProzent(new BigDecimal("2.50"));

        KrankenkasseDto saved = service.save(dto);

        assertThat(saved.getId()).isEqualTo(42L);
        assertThat(saved.getName()).isEqualTo("Test-KK");
        assertThat(saved.getZusatzbeitragProzent()).isEqualByComparingTo("2.50");
        assertThat(saved.getAktiv()).isTrue();
    }

    @Test
    void updateUebernimmtFelderUndAktivFlag() {
        Krankenkasse bestehend = new Krankenkasse();
        bestehend.setId(7L);
        bestehend.setName("Alt");
        bestehend.setZusatzbeitragProzent(new BigDecimal("2.00"));
        bestehend.setAktiv(true);
        when(repository.findById(7L)).thenReturn(java.util.Optional.of(bestehend));

        KrankenkasseDto dto = new KrankenkasseDto();
        dto.setId(7L);
        dto.setName("Neu");
        dto.setZusatzbeitragProzent(new BigDecimal("3.10"));
        dto.setAktiv(false);

        KrankenkasseDto saved = service.save(dto);

        assertThat(saved.getId()).isEqualTo(7L);
        assertThat(saved.getName()).isEqualTo("Neu");
        assertThat(saved.getZusatzbeitragProzent()).isEqualByComparingTo("3.10");
        assertThat(saved.getAktiv()).isFalse();
    }

    @Test
    void findAllSortiertNachName() {
        Krankenkasse a = krankenkasse(1L, "AOK");
        Krankenkasse t = krankenkasse(2L, "TK");
        when(repository.findAllByOrderByNameAsc()).thenReturn(List.of(a, t));

        List<KrankenkasseDto> list = service.findAll();

        assertThat(list).extracting(KrankenkasseDto::getName).containsExactly("AOK", "TK");
    }

    @Test
    void findAktivLiefertNurAktive() {
        Krankenkasse a = krankenkasse(1L, "AOK");
        when(repository.findByAktivTrueOrderByNameAsc()).thenReturn(List.of(a));

        assertThat(service.findAktiv()).hasSize(1);
    }

    @Test
    void deleteRuftRepoAufRichtigeId() {
        service.delete(99L);
        verify(repository).deleteById(99L);
    }

    private static Krankenkasse krankenkasse(Long id, String name) {
        Krankenkasse k = new Krankenkasse();
        k.setId(id);
        k.setName(name);
        k.setZusatzbeitragProzent(new BigDecimal("2.50"));
        k.setAktiv(true);
        return k;
    }
}
