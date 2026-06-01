package org.example.kalkulationsprogramm.service;

import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokument;
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokumentTyp;
import org.example.kalkulationsprogramm.domain.DokumentFreigabe;
import org.example.kalkulationsprogramm.domain.FreigabeQuellTyp;
import org.example.kalkulationsprogramm.domain.FreigabeStatus;
import org.example.kalkulationsprogramm.repository.AnfrageDokumentRepository;
import org.example.kalkulationsprogramm.repository.AnfrageRepository;
import org.example.kalkulationsprogramm.repository.AusgangsGeschaeftsDokumentRepository;
import org.example.kalkulationsprogramm.repository.DokumentFreigabeRepository;
import org.example.kalkulationsprogramm.repository.ProjektDokumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DokumentFreigabeServiceTest {

    @Mock
    private DokumentFreigabeRepository repository;
    @Mock
    private AnfrageDokumentRepository anfrageDokumentRepository;
    @Mock
    private ProjektDokumentRepository projektDokumentRepository;
    @Mock
    private AusgangsGeschaeftsDokumentRepository ausgangsGeschaeftsDokumentRepository;
    @Mock
    private AusgangsGeschaeftsDokumentService ausgangsGeschaeftsDokumentService;
    @Mock
    private AusgangsGeschaeftsDokumentAuditService ausgangsGeschaeftsDokumentAuditService;
    @Mock
    private WebPushService webPushService;
    @Mock
    private DateiSpeicherService dateiSpeicherService;
    @Mock
    private AutoAuftragsbestaetigungVersandService autoAuftragsbestaetigungVersandService;
    @Mock
    private ProjektManagementService projektManagementService;
    @Mock
    private AnfrageRepository anfrageRepository;

    @InjectMocks
    private DokumentFreigabeService service;

    /**
     * Regression: Filter "Angebot angenommen" zeigte 0 Treffer, weil der Service
     * nur Freigaben mit QuellTyp ANFRAGE (altes System) berücksichtigte. Neue
     * Angebote werden im AusgangsGeschaeftsDokument-System geführt und tragen
     * QuellTyp AUSGANGS_DOKUMENT.
     */
    @Test
    void findJuengsteProAnfrage_findetAcceptedAuchFuerAusgangsGeschaeftsDokumente() {
        Long anfrageId = 42L;
        Long ausgangsDokId = 700L;

        when(anfrageDokumentRepository.findGeschaeftsdokumentIdMappingByAnfrageIds(List.of(anfrageId)))
                .thenReturn(List.of());
        List<Object[]> mappingNeu = List.<Object[]>of(new Object[] { ausgangsDokId, anfrageId });
        when(ausgangsGeschaeftsDokumentRepository.findIdAnfrageIdMappingByAnfrageIds(List.of(anfrageId)))
                .thenReturn(mappingNeu);

        DokumentFreigabe freigabe = new DokumentFreigabe();
        freigabe.setQuellTyp(FreigabeQuellTyp.AUSGANGS_DOKUMENT);
        freigabe.setQuellDokumentId(ausgangsDokId);
        freigabe.setStatus(FreigabeStatus.ACCEPTED);
        freigabe.setErstelltAm(LocalDateTime.now().minusDays(1));
        freigabe.setAkzeptiertAm(LocalDateTime.now());
        when(repository.findByQuelle(eq(FreigabeQuellTyp.AUSGANGS_DOKUMENT), eq(List.of(ausgangsDokId))))
                .thenReturn(List.of(freigabe));

        Map<Long, DokumentFreigabe> result = service.findJuengsteProAnfrage(List.of(anfrageId));

        assertThat(result).containsKey(anfrageId);
        assertThat(result.get(anfrageId).getStatus()).isEqualTo(FreigabeStatus.ACCEPTED);
    }

    @Test
    void findJuengsteProAnfrage_leereListeLiefertLeereMap() {
        assertThat(service.findJuengsteProAnfrage(List.of())).isEmpty();
    }

    // ============== Beweissicherung: Vor- und Nachname bei Annahme ==============

    @Test
    void akzeptiere_ohneVorname_wirftIllegalArgumentException() {
        // Service-Check: Auch wenn die Bean-Validation umgangen wird (z.B. interner
        // Direktaufruf), darf eine Annahme nicht ohne Namen durchlaufen.
        DokumentFreigabe pending = pendingFreigabe("uuid-1");
        when(repository.findByUuid("uuid-1")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() ->
                service.akzeptiere("uuid-1", "1.2.3.4", "UA", "max@mustermann.de",
                        "   ", "Mustermann", "Mustermann"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vor- und Nachname");
    }

    @Test
    void akzeptiere_ohneNachname_wirftIllegalArgumentException() {
        DokumentFreigabe pending = pendingFreigabe("uuid-2");
        when(repository.findByUuid("uuid-2")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() ->
                service.akzeptiere("uuid-2", "1.2.3.4", "UA", null,
                        "Max", "", "Max"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vor- und Nachname");
    }

    @Test
    void akzeptiere_speichertNormalisiertenVorUndNachnameSowieZusammengesetztenAnzeigeName() {
        DokumentFreigabe pending = pendingFreigabe("uuid-3");
        when(repository.findByUuid("uuid-3")).thenReturn(Optional.of(pending));
        when(repository.save(any(DokumentFreigabe.class))).thenAnswer(inv -> inv.getArgument(0));
        // Auto-AB-Pfad inaktiv lassen: kein AusgangsGeschaeftsDokument finden.
        when(ausgangsGeschaeftsDokumentRepository.findById(any())).thenReturn(Optional.empty());

        DokumentFreigabe result = service.akzeptiere(
                "uuid-3", "1.2.3.4", "UA", "max@mustermann.de",
                "  Max   ", "  Mustermann\t", "");

        assertThat(result.getStatus()).isEqualTo(FreigabeStatus.ACCEPTED);
        assertThat(result.getUnterzeichnerVorname()).isEqualTo("Max");
        assertThat(result.getUnterzeichnerNachname()).isEqualTo("Mustermann");
        assertThat(result.getUnterzeichnerName()).isEqualTo("Max Mustermann");
        assertThat(result.getAkzeptiertAm()).isNotNull();
        assertThat(result.getHashAcceptance()).isNotBlank();
    }

    @Test
    void akzeptiere_idempotent_doppelteAnnahmeAendertNamensfelderNicht() {
        // Erster Klick hat bereits ACCEPTED inkl. Namen, Hash und Zeitstempel.
        DokumentFreigabe bereits = new DokumentFreigabe();
        bereits.setUuid("uuid-4");
        bereits.setStatus(FreigabeStatus.ACCEPTED);
        bereits.setUnterzeichnerVorname("Max");
        bereits.setUnterzeichnerNachname("Mustermann");
        bereits.setUnterzeichnerName("Max Mustermann");
        bereits.setAkzeptiertAm(LocalDateTime.now().minusMinutes(10));
        bereits.setAkzeptiertIp("1.1.1.1");
        bereits.setHashAcceptance("hash-vom-ersten-klick");
        when(repository.findByUuid("uuid-4")).thenReturn(Optional.of(bereits));

        DokumentFreigabe result = service.akzeptiere(
                "uuid-4", "9.9.9.9", "neuer UA", "anders@mustermann.de",
                "Erika", "Musterfrau", "Erika Musterfrau");

        // Felder bleiben unverändert — der erste Klick ist der Beweis.
        assertThat(result.getUnterzeichnerVorname()).isEqualTo("Max");
        assertThat(result.getUnterzeichnerNachname()).isEqualTo("Mustermann");
        assertThat(result.getUnterzeichnerName()).isEqualTo("Max Mustermann");
        assertThat(result.getAkzeptiertIp()).isEqualTo("1.1.1.1");
        assertThat(result.getHashAcceptance()).isEqualTo("hash-vom-ersten-klick");
    }

    /**
     * Nachtragsangebote sind – wie Angebote – digital freigebbar. Vor dieser
     * Erweiterung lieferte der Service nur für ANGEBOT einen Freigabe-Block.
     */
    @Test
    void erstelleFreigabeBlock_fuerNachtragsangebot_liefertFreigabeBlock() {
        AusgangsGeschaeftsDokument nachtrag = new AusgangsGeschaeftsDokument();
        nachtrag.setId(42L);
        nachtrag.setTyp(AusgangsGeschaeftsDokumentTyp.NACHTRAGSANGEBOT);
        nachtrag.setDokumentNummer("NA-2026/06/00001");
        when(ausgangsGeschaeftsDokumentRepository.findById(42L)).thenReturn(Optional.of(nachtrag));
        when(repository.findByQuelle(any(), any())).thenReturn(List.of());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<String> block = service.erstelleFreigabeBlockFuerDokument(
                42L, false, "test@example.com", null);

        assertThat(block).isPresent();
        assertThat(block.get()).contains("Nachtragsangebot");
    }

    /**
     * Helper: PENDING-Freigabe mit Hash und Salt-Init, sodass {@code akzeptiere}
     * den Acceptance-Hash berechnen kann ohne NPE.
     */
    private DokumentFreigabe pendingFreigabe(String uuid) {
        DokumentFreigabe f = new DokumentFreigabe();
        f.setUuid(uuid);
        f.setQuellTyp(FreigabeQuellTyp.AUSGANGS_DOKUMENT);
        f.setQuellDokumentId(123L);
        f.setDokumentNummer("ANG-2026-0001");
        f.setDokumentArt("Angebot");
        f.setKundeName("Mustermann GmbH");
        f.setStatus(FreigabeStatus.PENDING);
        f.setErstelltAm(LocalDateTime.now().minusHours(1));
        f.setAblaufDatum(LocalDateTime.now().plusDays(14));
        f.setHashOriginal("a".repeat(64));
        // hashSalt ist @Value-injiziert → für Tests via Reflection setzen.
        ReflectionTestUtils.setField(service, "hashSalt", "TEST_SALT");
        return f;
    }
}
