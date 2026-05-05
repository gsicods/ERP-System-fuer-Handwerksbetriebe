package org.example.kalkulationsprogramm.service;

import lombok.RequiredArgsConstructor;
import org.example.kalkulationsprogramm.domain.AnfrageGeschaeftsdokument;
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokument;
import org.example.kalkulationsprogramm.domain.AusgangsGeschaeftsDokumentTyp;
import org.example.kalkulationsprogramm.domain.DokumentFreigabe;
import org.example.kalkulationsprogramm.domain.FreigabeQuellTyp;
import org.example.kalkulationsprogramm.domain.FreigabeStatus;
import org.example.kalkulationsprogramm.domain.ProjektGeschaeftsdokument;
import org.example.kalkulationsprogramm.repository.AnfrageDokumentRepository;
import org.example.kalkulationsprogramm.repository.AusgangsGeschaeftsDokumentRepository;
import org.example.kalkulationsprogramm.repository.DokumentFreigabeRepository;
import org.example.kalkulationsprogramm.repository.ProjektDokumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Verwaltet die digitale Freigabe von Geschäftsdokumenten (Angebot, Auftragsbestätigung)
 * über einen UUID-Token, der dem Kunden per E-Mail zugesandt wird.
 *
 * Sicherheitsmodell:
 *  - Beim Versand wird ein SHA-256-Hash über die Geschäftsdaten + serverseitiges Salt
 *    gespeichert ("Fingerabdruck zum Versand-Zeitpunkt").
 *  - Beim Annehmen wird ein zweiter Hash über Original-Hash + Akzeptanzdaten erstellt
 *    und als unveränderbarer Beweis gespeichert.
 *  - Standardgültigkeit: 14 Tage ab Versand.
 */
@Service
@RequiredArgsConstructor
public class DokumentFreigabeService
{
    private static final int GUELTIGKEITS_TAGE = 14;

    private final DokumentFreigabeRepository repository;
    private final AnfrageDokumentRepository anfrageDokumentRepository;
    private final ProjektDokumentRepository projektDokumentRepository;
    private final AusgangsGeschaeftsDokumentRepository ausgangsGeschaeftsDokumentRepository;
    private final WebPushService webPushService;

    @Value("${freigabe.hash.salt:CHANGE_ME_LOCAL_ONLY}")
    private String hashSalt;

    @Value("${freigabe.public-base-url:https://bauschlosserei-kuhn.de}")
    private String publicBaseUrl;

    /**
     * Erzeugt einen Freigabe-Token für ein Anfrage-Geschäftsdokument
     * (Angebot oder Auftragsbestätigung an einer offenen Anfrage).
     */
    @Transactional
    public DokumentFreigabe erstelleFuerAnfrage(AnfrageGeschaeftsdokument dokument, String kundeName, String kundeEmail)
    {
        DokumentFreigabe freigabe = baseFreigabe();
        freigabe.setQuellTyp(FreigabeQuellTyp.ANFRAGE);
        freigabe.setQuellDokumentId(dokument.getId());
        freigabe.setDokumentNummer(dokument.getDokumentid());
        freigabe.setDokumentArt(dokument.getGeschaeftsdokumentart());
        freigabe.setDokumentBetrag(dokument.getBruttoBetrag());
        freigabe.setDokumentDatei(dokument.getGespeicherterDateiname());
        freigabe.setBauvorhaben(dokument.getAnfrage() != null ? dokument.getAnfrage().getBauvorhaben() : null);
        freigabe.setKundeName(kundeName);
        freigabe.setKundeEmail(kundeEmail);
        freigabe.setHashOriginal(berechneHashOriginal(freigabe));
        return repository.save(freigabe);
    }

    /**
     * Erzeugt einen Freigabe-Token für ein Projekt-Geschäftsdokument.
     */
    @Transactional
    public DokumentFreigabe erstelleFuerProjekt(ProjektGeschaeftsdokument dokument, String kundeName, String kundeEmail)
    {
        DokumentFreigabe freigabe = baseFreigabe();
        freigabe.setQuellTyp(FreigabeQuellTyp.PROJEKT);
        freigabe.setQuellDokumentId(dokument.getId());
        freigabe.setDokumentNummer(dokument.getDokumentid());
        freigabe.setDokumentArt(dokument.getGeschaeftsdokumentart());
        freigabe.setDokumentBetrag(dokument.getBruttoBetrag());
        freigabe.setDokumentDatei(dokument.getGespeicherterDateiname());
        freigabe.setBauvorhaben(dokument.getProjekt() != null ? dokument.getProjekt().getBauvorhaben() : null);
        freigabe.setKundeName(kundeName);
        freigabe.setKundeEmail(kundeEmail);
        freigabe.setHashOriginal(berechneHashOriginal(freigabe));
        return repository.save(freigabe);
    }

    /**
     * Liefert die fertige öffentliche URL, die in die E-Mail eingefügt wird.
     */
    public String buildPublicUrl(DokumentFreigabe freigabe)
    {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
        return base + "/freigabe/" + freigabe.getUuid();
    }

    /**
     * Erzeugt den HTML-Block für die E-Mail, der den Freigabe-Link enthält.
     * Wird sowohl beim E-Mail-Versand als auch bei der Template-Vorschau genutzt.
     */
    public static String buildFreigabeBlockHtml(String url, String dokumentArt)
    {
        String art = dokumentArt == null || dokumentArt.isBlank() ? "Dokument" : dokumentArt;
        return "<div style=\"margin:24px 0;padding:16px 18px;border-left:3px solid #500010;background:#fafafa;font-family:Arial,Helvetica,sans-serif;\">"
                + "<p style=\"margin:0 0 6px 0;font-weight:600;color:#1e293b;\">" + art + " digital prüfen und annehmen</p>"
                + "<p style=\"margin:0 0 10px 0;color:#475569;line-height:1.45;\">"
                + "Sie können dieses " + art + " bequem online ansehen und mit einem Klick verbindlich annehmen:"
                + "</p>"
                + "<p style=\"margin:0;\"><a href=\"" + url + "\" style=\"color:#500010;font-weight:600;text-decoration:underline;\">"
                + url + "</a></p>"
                + "<p style=\"margin:8px 0 0 0;color:#94a3b8;font-size:13px;\">Der Link ist 14 Tage gültig.</p>"
                + "</div>";
    }

    /**
     * Erzeugt einen Freigabe-Token für ein AusgangsGeschaeftsDokument (neues Dokumentsystem).
     */
    @Transactional
    public DokumentFreigabe erstelleFuerAusgangsGeschaeftsDokument(AusgangsGeschaeftsDokument dok, String kundeEmail)
    {
        DokumentFreigabe freigabe = baseFreigabe();
        freigabe.setQuellTyp(FreigabeQuellTyp.AUSGANGS_DOKUMENT);
        freigabe.setQuellDokumentId(dok.getId());
        freigabe.setDokumentNummer(dok.getDokumentNummer());
        freigabe.setDokumentArt(typZuBezeichnung(dok.getTyp()));
        freigabe.setDokumentBetrag(dok.getBetragBrutto());
        freigabe.setDokumentDatei(null); // PDFs werden on-demand generiert

        String bauvorhaben = null;
        String kundeName = null;
        if (dok.getProjekt() != null)
        {
            bauvorhaben = dok.getProjekt().getBauvorhaben();
            if (dok.getProjekt().getKundenId() != null)
            {
                kundeName = dok.getProjekt().getKundenId().getName();
            }
        }
        else if (dok.getAnfrage() != null)
        {
            bauvorhaben = dok.getAnfrage().getBauvorhaben();
            if (dok.getAnfrage().getKunde() != null)
            {
                kundeName = dok.getAnfrage().getKunde().getName();
            }
        }
        if (kundeName == null && dok.getKunde() != null)
        {
            kundeName = dok.getKunde().getName();
        }

        freigabe.setBauvorhaben(bauvorhaben);
        freigabe.setKundeName(kundeName);
        freigabe.setKundeEmail(kundeEmail);
        freigabe.setHashOriginal(berechneHashOriginal(freigabe));
        return repository.save(freigabe);
    }

    /**
     * Erstellt einen Freigabe-Token für ein Dokument (per ID) und gibt den fertigen
     * HTML-Block zurück, der in die E-Mail-Vorlage eingebettet werden kann.
     * Sucht zuerst im neuen System (AusgangsGeschaeftsDokument), dann im alten.
     * Nur für Angebote und Auftragsbestätigungen – bei anderen Typen wird Optional.empty() zurückgegeben.
     */
    @Transactional
    public Optional<String> erstelleFreigabeBlockFuerDokument(Long dokumentId, boolean isAnfrage, String recipient)
    {
        if (dokumentId == null) return Optional.empty();
        try
        {
            // Neues System: AusgangsGeschaeftsDokument (DocumentEditor)
            Optional<AusgangsGeschaeftsDokument> agdOpt = ausgangsGeschaeftsDokumentRepository.findById(dokumentId);
            if (agdOpt.isPresent())
            {
                AusgangsGeschaeftsDokument agd = agdOpt.get();
                if (!istAngebotOderABTyp(agd.getTyp())) return Optional.empty();
                DokumentFreigabe freigabe = erstelleFuerAusgangsGeschaeftsDokument(agd, recipient);
                return Optional.of(buildFreigabeBlockHtml(buildPublicUrl(freigabe), typZuBezeichnung(agd.getTyp())));
            }

            // Fallback: altes System
            if (isAnfrage)
            {
                return anfrageDokumentRepository.findById(dokumentId)
                        .filter(d -> d instanceof AnfrageGeschaeftsdokument)
                        .filter(d -> istAngebotOderAB(((AnfrageGeschaeftsdokument) d).getGeschaeftsdokumentart()))
                        .map(d -> {
                            AnfrageGeschaeftsdokument gesDoc = (AnfrageGeschaeftsdokument) d;
                            String kundeName = gesDoc.getAnfrage() != null && gesDoc.getAnfrage().getKunde() != null
                                    ? gesDoc.getAnfrage().getKunde().getName() : null;
                            DokumentFreigabe freigabe = erstelleFuerAnfrage(gesDoc, kundeName, recipient);
                            return buildFreigabeBlockHtml(buildPublicUrl(freigabe), gesDoc.getGeschaeftsdokumentart());
                        });
            }
            else
            {
                return projektDokumentRepository.findById(dokumentId)
                        .filter(d -> d instanceof ProjektGeschaeftsdokument)
                        .filter(d -> istAngebotOderAB(((ProjektGeschaeftsdokument) d).getGeschaeftsdokumentart()))
                        .map(d -> {
                            ProjektGeschaeftsdokument gesDoc = (ProjektGeschaeftsdokument) d;
                            String kundeName = gesDoc.getProjekt() != null && gesDoc.getProjekt().getKundenId() != null
                                    ? gesDoc.getProjekt().getKundenId().getName() : null;
                            DokumentFreigabe freigabe = erstelleFuerProjekt(gesDoc, kundeName, recipient);
                            return buildFreigabeBlockHtml(buildPublicUrl(freigabe), gesDoc.getGeschaeftsdokumentart());
                        });
            }
        }
        catch (Exception e)
        {
            return Optional.empty();
        }
    }

    private static boolean istAngebotOderABTyp(AusgangsGeschaeftsDokumentTyp typ)
    {
        return typ == AusgangsGeschaeftsDokumentTyp.ANGEBOT || typ == AusgangsGeschaeftsDokumentTyp.AUFTRAGSBESTAETIGUNG;
    }

    private static String typZuBezeichnung(AusgangsGeschaeftsDokumentTyp typ)
    {
        if (typ == null) return "Dokument";
        return switch (typ)
        {
            case ANGEBOT -> "Angebot";
            case AUFTRAGSBESTAETIGUNG -> "Auftragsbestätigung";
            default -> typ.name();
        };
    }

    private static boolean istAngebotOderAB(String art)
    {
        if (art == null) return false;
        String lower = art.toLowerCase(Locale.GERMAN);
        return lower.contains("angebot") || lower.contains("auftragsbest");
    }

    /**
     * Lädt eine Freigabe anhand der UUID und aktualisiert den Status auf EXPIRED,
     * falls das Ablaufdatum überschritten ist.
     */
    @Transactional
    public Optional<DokumentFreigabe> findByUuidUndAktualisiereStatus(String uuid)
    {
        return repository.findByUuid(uuid).map(freigabe ->
        {
            if (freigabe.getStatus() == FreigabeStatus.PENDING && freigabe.istAbgelaufen())
            {
                freigabe.setStatus(FreigabeStatus.EXPIRED);
                repository.save(freigabe);
            }
            return freigabe;
        });
    }

    /**
     * Markiert eine Freigabe als digital akzeptiert. Speichert IP, User-Agent und
     * berechnet einen unveränderbaren Acceptance-Hash als Beweis.
     */
    @Transactional
    public DokumentFreigabe akzeptiere(String uuid, String ip, String userAgent, String email)
    {
        DokumentFreigabe freigabe = repository.findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Unbekannte Freigabe-UUID"));

        if (freigabe.getStatus() == FreigabeStatus.ACCEPTED)
        {
            return freigabe;
        }
        if (freigabe.getStatus() != FreigabeStatus.PENDING)
        {
            throw new IllegalStateException("Freigabe ist nicht mehr gültig: " + freigabe.getStatus());
        }
        if (freigabe.istAbgelaufen())
        {
            freigabe.setStatus(FreigabeStatus.EXPIRED);
            repository.save(freigabe);
            throw new IllegalStateException("Freigabe ist abgelaufen");
        }

        LocalDateTime jetzt = LocalDateTime.now();
        freigabe.setStatus(FreigabeStatus.ACCEPTED);
        freigabe.setAkzeptiertAm(jetzt);
        freigabe.setAkzeptiertIp(ip);
        freigabe.setAkzeptiertUserAgent(userAgent);
        freigabe.setAkzeptiertEmail(email);
        freigabe.setHashAcceptance(berechneHashAcceptance(freigabe, ip, email, jetzt));
        DokumentFreigabe saved = repository.save(freigabe);

        // Push-Notification an alle registrierten Geräte (Büro), damit jemand sofort
        // sieht, dass der Kunde digital angenommen hat.
        try {
            String art = saved.getDokumentArt() == null ? "Dokument" : saved.getDokumentArt();
            String kunde = saved.getKundeName() == null || saved.getKundeName().isBlank()
                    ? (saved.getKundeEmail() == null ? "Ein Kunde" : saved.getKundeEmail())
                    : saved.getKundeName();
            String body = kunde + " hat " + art + " " + saved.getDokumentNummer() + " digital angenommen.";
            webPushService.notifyAll(art + " angenommen", body, "/anfragen?freigabe=" + saved.getUuid());
        } catch (Exception ignored) {
            // Push-Probleme dürfen die Annahme nie blockieren
        }
        return saved;
    }

    /**
     * Lädt für eine Liste von Quell-Dokument-IDs jeweils die jüngste Freigabe
     * (bevorzugt ACCEPTED, sonst aktuellste). Wird für Status-Badges in
     * Anfrage- bzw. Projekt-Übersichten genutzt.
     */
    @Transactional(readOnly = true)
    public Map<Long, DokumentFreigabe> findJuengsteProQuelle(FreigabeQuellTyp typ, List<Long> quellDokumentIds)
    {
        if (quellDokumentIds == null || quellDokumentIds.isEmpty())
        {
            return Map.of();
        }
        List<DokumentFreigabe> alle = repository.findByQuelle(typ, quellDokumentIds);
        return alle.stream().collect(Collectors.toMap(
                DokumentFreigabe::getQuellDokumentId,
                f -> f,
                DokumentFreigabeService::pickRelevant
        ));
    }

    /**
     * Findet pro Anfrage-ID die relevanteste Freigabe (akzeptiert &gt; pending &gt; abgelaufen,
     * sonst aktuellste). Joint die Anfrage-Geschäftsdokumente, weil DokumentFreigabe nur
     * die Quell-Dokument-ID kennt, nicht die Container-Anfrage.
     */
    @Transactional(readOnly = true)
    public Map<Long, DokumentFreigabe> findJuengsteProAnfrage(List<Long> anfrageIds)
    {
        if (anfrageIds == null || anfrageIds.isEmpty())
        {
            return Map.of();
        }
        List<Object[]> mapping = anfrageDokumentRepository.findGeschaeftsdokumentIdMappingByAnfrageIds(anfrageIds);
        return aggregiereProContainer(FreigabeQuellTyp.ANFRAGE, mapping);
    }

    /**
     * Pendant zu {@link #findJuengsteProAnfrage} für Projekte.
     */
    @Transactional(readOnly = true)
    public Map<Long, DokumentFreigabe> findJuengsteProProjekt(List<Long> projektIds)
    {
        if (projektIds == null || projektIds.isEmpty())
        {
            return Map.of();
        }
        List<Object[]> mapping = projektDokumentRepository.findGeschaeftsdokumentIdMappingByProjektIds(projektIds);
        return aggregiereProContainer(FreigabeQuellTyp.PROJEKT, mapping);
    }

    private Map<Long, DokumentFreigabe> aggregiereProContainer(FreigabeQuellTyp typ, List<Object[]> dokToContainerMapping)
    {
        if (dokToContainerMapping.isEmpty())
        {
            return Map.of();
        }
        Map<Long, Long> dokIdZuContainerId = new java.util.HashMap<>();
        for (Object[] row : dokToContainerMapping)
        {
            Long dokId = (Long) row[0];
            Long containerId = (Long) row[1];
            dokIdZuContainerId.put(dokId, containerId);
        }
        List<DokumentFreigabe> freigaben = repository.findByQuelle(typ, new java.util.ArrayList<>(dokIdZuContainerId.keySet()));
        Map<Long, DokumentFreigabe> result = new java.util.HashMap<>();
        for (DokumentFreigabe f : freigaben)
        {
            // EXPIRED-Update: Im Read-Path nicht persistieren – Wert ist trotzdem korrekt
            // (PENDING-Freigaben mit überschrittenem Ablauf werden vom GET-Endpoint
            // beim nächsten Abruf in der DB aktualisiert).
            FreigabeStatus effektiverStatus = f.getStatus() == FreigabeStatus.PENDING && f.istAbgelaufen()
                    ? FreigabeStatus.EXPIRED : f.getStatus();
            f.setStatus(effektiverStatus);
            Long containerId = dokIdZuContainerId.get(f.getQuellDokumentId());
            if (containerId == null) continue;
            result.merge(containerId, f, DokumentFreigabeService::pickRelevant);
        }
        return result;
    }

    private static DokumentFreigabe pickRelevant(DokumentFreigabe a, DokumentFreigabe b)
    {
        if (a.getStatus() == FreigabeStatus.ACCEPTED) return a;
        if (b.getStatus() == FreigabeStatus.ACCEPTED) return b;
        if (a.getStatus() == FreigabeStatus.PENDING && b.getStatus() != FreigabeStatus.PENDING) return a;
        if (b.getStatus() == FreigabeStatus.PENDING && a.getStatus() != FreigabeStatus.PENDING) return b;
        return a.getErstelltAm().isAfter(b.getErstelltAm()) ? a : b;
    }

    private DokumentFreigabe baseFreigabe()
    {
        DokumentFreigabe freigabe = new DokumentFreigabe();
        freigabe.setUuid(UUID.randomUUID().toString());
        freigabe.setErstelltAm(LocalDateTime.now());
        freigabe.setAblaufDatum(LocalDateTime.now().plusDays(GUELTIGKEITS_TAGE));
        freigabe.setStatus(FreigabeStatus.PENDING);
        return freigabe;
    }

    private String berechneHashOriginal(DokumentFreigabe f)
    {
        BigDecimal betrag = f.getDokumentBetrag();
        String input = String.join("|",
                f.getQuellTyp() == null ? "" : f.getQuellTyp().name(),
                String.valueOf(f.getQuellDokumentId()),
                f.getDokumentNummer() == null ? "" : f.getDokumentNummer(),
                f.getDokumentArt() == null ? "" : f.getDokumentArt(),
                betrag == null ? "" : betrag.toPlainString(),
                f.getKundeEmail() == null ? "" : f.getKundeEmail(),
                hashSalt
        );
        return sha256Hex(input);
    }

    private String berechneHashAcceptance(DokumentFreigabe freigabe, String ip, String email, LocalDateTime zeitpunkt)
    {
        String input = String.join("|",
                freigabe.getHashOriginal(),
                freigabe.getUuid(),
                ip == null ? "" : ip,
                email == null ? "" : email,
                zeitpunkt.toString(),
                hashSalt
        );
        return sha256Hex(input);
    }

    private static String sha256Hex(String input)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-256 nicht verfügbar", e);
        }
    }
}
