package org.example.kalkulationsprogramm.service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.email.EmailService;
import org.example.kalkulationsprogramm.domain.Anfrage;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Versendet die automatische Bestätigungsmail an Leads, die über den
 * öffentlichen Webseiten-Funnel eine Anfrage abschicken. Trigger:
 * {@link AnfrageFunnelService#verarbeiteFunnelAnfrage}.
 *
 * <p>Architektur-Vorbild: {@link AutoAuftragsbestaetigungVersandService} —
 * gleicher Aufbau, gleicher Versand-Pfad (Template → Signatur → Absender →
 * SMTP). So bleibt das Verhalten konsistent zu allen anderen
 * System-generierten Mails.</p>
 *
 * <p><b>Fehlerverhalten:</b> Alle Exceptions werden geschluckt und geloggt. Die
 * Bestätigungsmail ist Komfort, kein harter Bestandteil der Funnel-Verarbeitung
 * — ein SMTP-Ausfall darf nicht dazu führen, dass der Lead-Datensatz im ERP
 * verloren geht. Der Aufruf erfolgt deshalb innerhalb der bestehenden
 * Transaktion, ohne sie scheitern zu lassen.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnfrageBestaetigungVersandService {

    /**
     * Dokumenttyp-Key der DB-Vorlage. Bewusst eigener Wert (kein Dokumenttyp-
     * Enum-Member), weil eine Lead-Bestätigung kein Geschäftsdokument ist —
     * sie wird im UI nur über die Kategorie WEBSITE gruppiert.
     */
    public static final String VORLAGE_DOKUMENT_TYP = "WEBSITE_ANFRAGE_BESTAETIGUNG";

    private static final DateTimeFormatter DATUM_DE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final EmailTextTemplateService emailTextTemplateService;
    private final EmailSignatureService emailSignatureService;
    private final SystemSettingsService systemSettingsService;

    /**
     * Versendet eine Bestätigungsmail an den ersten in der Anfrage hinterlegten
     * Kunden-E-Mail-Empfänger. Liefert {@code true} bei erfolgreichem Versand,
     * {@code false} bei jedem Problem (kein Empfänger, keine aktive Vorlage,
     * SMTP-Fehler …). Wirft nie.
     *
     * @param anfrage  persistierte Funnel-Anfrage
     * @param vorname  Vorname aus dem Funnel-Payload (für ANREDE/KUNDENNAME)
     * @param nachname Nachname aus dem Funnel-Payload
     * @param nachricht freier Beschreibungstext des Leads (wird HTML-escaped)
     */
    public boolean versendeBestaetigung(Anfrage anfrage, String vorname, String nachname, String nachricht) {
        if (anfrage == null) {
            return false;
        }
        String empfaenger = ersterEmpfaenger(anfrage);
        if (empfaenger == null) {
            log.info("Anfrage-Bestaetigung uebersprungen: keine Empfaenger-Mail in Anfrage id={}", anfrage.getId());
            return false;
        }

        try {
            Map<String, String> ctx = baueKontext(anfrage, vorname, nachname, nachricht);
            EmailService.EmailContent content = emailTextTemplateService.render(VORLAGE_DOKUMENT_TYP, ctx);
            if (content == null || content.subject() == null || content.subject().isBlank()) {
                log.warn("Anfrage-Bestaetigung uebersprungen: keine aktive Vorlage '{}'", VORLAGE_DOKUMENT_TYP);
                return false;
            }

            if (!systemSettingsService.isSmtpConfigured()) {
                log.warn("Anfrage-Bestaetigung uebersprungen: SMTP ist nicht konfiguriert");
                return false;
            }

            String htmlMitSignatur = emailSignatureService
                    .appendSystemSignatureIfConfigured(content.htmlBody());
            String absender = systemSettingsService.getMailFromAddress();

            EmailService emailService = baueEmailService();
            emailService.sendEmail(
                    empfaenger,
                    null,
                    absender,
                    content.subject(),
                    htmlMitSignatur,
                    null,
                    null);

            log.info("Anfrage-Bestaetigung an {} versendet (anfrageId={})", empfaenger, anfrage.getId());
            return true;
        } catch (Exception e) {
            // Bewusst Exception-fangen: die Funnel-Persistenz darf nicht
            // scheitern, nur weil SMTP/Template/Signatur kurz aerger machen.
            log.error("Anfrage-Bestaetigung fuer Anfrage {} fehlgeschlagen: {}",
                    anfrage.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Paketsichtbar, damit der Unit-Test einen Mock-EmailService injecten kann
     * ohne echten SMTP-Connect zu versuchen.
     */
    EmailService baueEmailService() {
        return new EmailService(
                systemSettingsService.getSmtpHost(),
                systemSettingsService.getSmtpPort(),
                systemSettingsService.getSmtpUsername(),
                systemSettingsService.getSmtpPassword());
    }

    private static String ersterEmpfaenger(Anfrage anfrage) {
        if (anfrage.getKundenEmails() == null) return null;
        return anfrage.getKundenEmails().stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    private static Map<String, String> baueKontext(Anfrage anfrage, String vorname, String nachname, String nachricht) {
        Map<String, String> ctx = new HashMap<>();
        String voll = (safe(vorname) + " " + safe(nachname)).trim();
        // Bei Leads kennen wir keine formale Anrede (Herr/Frau) — wir bleiben
        // mit "Hallo {Vorname Nachname}" bewusst informell, das passt zur
        // Handwerker-Tonalitaet und vermeidet falsch geratene Anreden.
        ctx.put("ANREDE", voll.isEmpty() ? "Hallo" : "Hallo " + escape(voll));
        ctx.put("KUNDENNAME", escape(voll));
        ctx.put("VORNAME", escape(safe(vorname)));
        ctx.put("NACHNAME", escape(safe(nachname)));
        ctx.put("BAUVORHABEN", escape(safe(anfrage.getBauvorhaben())));
        ctx.put("NACHRICHT", escape(safe(nachricht)));
        ctx.put("ANFRAGE_DATUM", anfrage.getAnlegedatum() != null
                ? anfrage.getAnlegedatum().format(DATUM_DE)
                : "");
        ctx.put("ANFRAGENUMMER", anfrage.getId() != null ? anfrage.getId().toString() : "");
        return ctx;
    }

    /**
     * HTML-escape gegen XSS — die Werte landen 1:1 im HTML-Body, der per
     * Mailclient gerendert wird. Ein boeswilliger Lead koennte sonst per
     * NACHRICHT-Feld Skript-Inhalte einschleusen, die der Empfaenger (der
     * Handwerker, der seine Anfragenmails sichtet) zu sehen bekommt.
     */
    private static String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
