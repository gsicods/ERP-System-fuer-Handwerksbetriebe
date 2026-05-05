package org.example.kalkulationsprogramm.service;

import lombok.RequiredArgsConstructor;
import org.example.kalkulationsprogramm.domain.AnfrageGeschaeftsdokument;
import org.example.kalkulationsprogramm.domain.DokumentFreigabe;
import org.example.kalkulationsprogramm.domain.FreigabeQuellTyp;
import org.example.kalkulationsprogramm.domain.FreigabeStatus;
import org.example.kalkulationsprogramm.domain.ProjektGeschaeftsdokument;
import org.example.kalkulationsprogramm.repository.AnfrageDokumentRepository;
import org.example.kalkulationsprogramm.repository.DokumentFreigabeRepository;
import org.example.kalkulationsprogramm.repository.ProjektDokumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
