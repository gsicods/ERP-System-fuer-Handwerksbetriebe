package org.example.kalkulationsprogramm.service;

import org.example.kalkulationsprogramm.domain.Lohnabrechnung;
import org.example.kalkulationsprogramm.domain.Mitarbeiter;
import org.example.kalkulationsprogramm.repository.LohnabrechnungRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests für {@link LohnabrechnungService}, insbesondere die PDF-Auflösung
 * für den Download-Endpoint.
 */
@ExtendWith(MockitoExtension.class)
class LohnabrechnungServiceTest {

    @Mock
    private LohnabrechnungRepository lohnabrechnungRepository;

    @InjectMocks
    private LohnabrechnungService service;

    @TempDir
    Path lohnabrechnungDir;

    @TempDir
    Path mailAttachmentDir;

    private Lohnabrechnung lohnabrechnung;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "lohnabrechnungDir", lohnabrechnungDir.toString());
        ReflectionTestUtils.setField(service, "mailAttachmentDir", mailAttachmentDir.toString());

        Mitarbeiter mitarbeiter = new Mitarbeiter();
        mitarbeiter.setVorname("Max");
        mitarbeiter.setNachname("Mustermann");

        lohnabrechnung = new Lohnabrechnung();
        lohnabrechnung.setMitarbeiter(mitarbeiter);
        lohnabrechnung.setJahr(2026);
        lohnabrechnung.setMonat(5);
        lohnabrechnung.setOriginalDateiname("Lohnabrechnungen Mai 2026.pdf");
        lohnabrechnung.setGespeicherterDateiname("abc-123-uuid.pdf");
    }

    /**
     * Regressionstest für den 404-Bug: Der Download suchte die Datei unter dem
     * Original-Dateinamen, gespeichert ist sie aber unter dem UUID-Namen.
     */
    @Test
    void findPdfNutztGespeichertenDateinamenStattOriginalnamen() throws Exception {
        Files.write(lohnabrechnungDir.resolve("abc-123-uuid.pdf"), new byte[]{1, 2, 3});
        when(lohnabrechnungRepository.findById(2L)).thenReturn(Optional.of(lohnabrechnung));

        Optional<LohnabrechnungService.PdfDatei> pdf = service.findPdf(2L);

        assertThat(pdf).isPresent();
        assertThat(pdf.get().pfad().getFileName().toString()).isEqualTo("abc-123-uuid.pdf");
        assertThat(pdf.get().anzeigeName()).isEqualTo("Lohnabrechnungen Mai 2026.pdf");
    }

    /**
     * Bestandsdaten referenzieren noch den Original-E-Mail-Anhang im
     * Mail-Attachment-Verzeichnis – auch der muss gefunden werden.
     */
    @Test
    void findPdfFindetAltdatenImMailAttachmentVerzeichnis() throws Exception {
        Files.write(mailAttachmentDir.resolve("abc-123-uuid.pdf"), new byte[]{1, 2, 3});
        when(lohnabrechnungRepository.findById(2L)).thenReturn(Optional.of(lohnabrechnung));

        Optional<LohnabrechnungService.PdfDatei> pdf = service.findPdf(2L);

        assertThat(pdf).isPresent();
        assertThat(pdf.get().pfad()).isEqualTo(
                mailAttachmentDir.resolve("abc-123-uuid.pdf").toAbsolutePath().normalize());
    }

    @Test
    void findPdfGibtLeerWennDateiNichtExistiert() {
        when(lohnabrechnungRepository.findById(2L)).thenReturn(Optional.of(lohnabrechnung));

        assertThat(service.findPdf(2L)).isEmpty();
    }

    @Test
    void findPdfGibtLeerWennLohnabrechnungUnbekannt() {
        when(lohnabrechnungRepository.findById(999L)).thenReturn(Optional.empty());

        assertThat(service.findPdf(999L)).isEmpty();
    }

    @Test
    void findPdfBlocktPathTraversal() {
        lohnabrechnung.setGespeicherterDateiname("../../etc/passwd");
        when(lohnabrechnungRepository.findById(2L)).thenReturn(Optional.of(lohnabrechnung));

        assertThat(service.findPdf(2L)).isEmpty();
    }
}
