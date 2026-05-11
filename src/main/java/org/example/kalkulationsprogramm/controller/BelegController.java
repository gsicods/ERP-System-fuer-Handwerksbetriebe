package org.example.kalkulationsprogramm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.kalkulationsprogramm.config.FrontendUserPrincipal;
import org.example.kalkulationsprogramm.domain.Beleg;
import org.example.kalkulationsprogramm.domain.BelegKategorie;
import org.example.kalkulationsprogramm.domain.BelegStatus;
import org.example.kalkulationsprogramm.domain.Mitarbeiter;
import org.example.kalkulationsprogramm.dto.BelegDto;
import org.example.kalkulationsprogramm.repository.MitarbeiterRepository;
import org.example.kalkulationsprogramm.service.BelegService;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST-Endpoints für das Beleg-Modul (Buchhaltung).
 *
 * Auth-Strategie:
 * - Mobile (react-zeiterfassung) authentifiziert per ?token=… (loginToken auf
 *   Mitarbeiter), wie bei LieferantenDokumente / Zeiterfassung.
 * - PC (react-pc-frontend) authentifiziert per Session-Cookie über Spring
 *   Security; der eingeloggte FrontendUserPrincipal wird auf einen
 *   Mitarbeiter via E-Mail/Username gemappt — wenn das fehlschlägt,
 *   verwenden wir den Token-Fallback.
 *
 * Berechtigungen kommen aus dem bestehenden System
 * (AbteilungDokumentBerechtigung mit dokument_typ='BELEG'), verwaltet unter
 * Administration → Lieferanten-Dokumentenrechte.
 */
@Slf4j
@RestController
@RequestMapping("/api/buchhaltung")
@RequiredArgsConstructor
public class BelegController {

    private final BelegService belegService;
    private final MitarbeiterRepository mitarbeiterRepository;

    // ===================== Helpers =====================

    private Mitarbeiter resolveCaller(String token, Authentication auth) {
        if (token != null && !token.isBlank()) {
            Mitarbeiter m = belegService.findByToken(token);
            if (m != null) return m;
        }
        if (auth != null && auth.getPrincipal() instanceof FrontendUserPrincipal p) {
            // PC: FrontendUser → Mitarbeiter über Email
            if (p.getUsername() != null) {
                return mitarbeiterRepository.findAll().stream()
                        .filter(m -> p.getUsername().equalsIgnoreCase(m.getEmail()))
                        .findFirst().orElse(null);
            }
        }
        return null;
    }

    private ResponseEntity<Map<String, String>> forbidden(String msg) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", msg));
    }

    // ===================== Permissions =====================

    @GetMapping("/me/permissions")
    public ResponseEntity<BelegDto.PermissionResponse> myPermissions(
            @RequestParam(value = "token", required = false) String token,
            Authentication auth) {
        Mitarbeiter m = resolveCaller(token, auth);
        if (m == null) {
            return ResponseEntity.ok(BelegDto.PermissionResponse.builder()
                    .darfSehen(false).darfScannen(false).build());
        }
        return ResponseEntity.ok(belegService.getPermissions(m));
    }

    // === Mobile-Spiegel: dieselben Endpoints, aber unter /mobile/ ===
    // Spring Security routet /api/buchhaltung/mobile/** in die token-only
    // zeiterfassungFilterChain — anders als /api/buchhaltung/** das Session-Auth
    // braucht. Die Mobile-PWA hat keine Session, nur einen loginToken.

    @GetMapping("/mobile/me/permissions")
    public ResponseEntity<BelegDto.PermissionResponse> myPermissionsMobile(
            @RequestParam(value = "token", required = false) String token) {
        return myPermissions(token, null);
    }

    @PostMapping(value = "/mobile/belege", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadBelegMobile(
            @RequestPart("datei") MultipartFile datei,
            @RequestParam(value = "token", required = false) String token) {
        return uploadBeleg(datei, token, null);
    }

    // ===================== Upload (Mobile + PC) =====================

    @PostMapping(value = "/belege", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadBeleg(
            @RequestPart("datei") MultipartFile datei,
            @RequestParam(value = "token", required = false) String token,
            Authentication auth) {
        Mitarbeiter caller = resolveCaller(token, auth);
        if (caller == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Nicht angemeldet"));
        }
        if (!belegService.darfScannen(caller)) {
            return forbidden("Keine Berechtigung zum Scannen von Belegen");
        }
        try {
            Beleg b = belegService.uploadBeleg(datei, caller);
            return ResponseEntity.ok(belegService.toDto(b));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Beleg-Upload fehlgeschlagen", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Upload fehlgeschlagen"));
        }
    }

    // ===================== Liste / Detail =====================

    @GetMapping("/belege")
    public ResponseEntity<List<BelegDto.Response>> listBelege(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "kategorie", required = false) String kategorie,
            @RequestParam(value = "token", required = false) String token,
            Authentication auth) {
        Mitarbeiter caller = resolveCaller(token, auth);
        if (caller == null || !belegService.darfSehen(caller)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        BelegStatus st = parseEnum(BelegStatus.class, status);
        BelegKategorie kat = parseEnum(BelegKategorie.class, kategorie);
        return ResponseEntity.ok(belegService.listBelege(st, kat));
    }

    @GetMapping("/belege/{id}")
    public ResponseEntity<BelegDto.Response> getBeleg(
            @PathVariable Long id,
            @RequestParam(value = "token", required = false) String token,
            Authentication auth) {
        Mitarbeiter caller = resolveCaller(token, auth);
        if (caller == null || !belegService.darfSehen(caller)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        BelegDto.Response r = belegService.getBeleg(id);
        return r == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(r);
    }

    @GetMapping("/belege/{id}/datei")
    public ResponseEntity<?> downloadDatei(
            @PathVariable Long id,
            @RequestParam(value = "token", required = false) String token,
            Authentication auth) {
        Mitarbeiter caller = resolveCaller(token, auth);
        if (caller == null || !belegService.darfSehen(caller)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Beleg beleg = belegService.getRawBeleg(id);
        if (beleg == null) return ResponseEntity.notFound().build();
        try {
            Path p = belegService.getBelegDatei(beleg);
            if (!Files.exists(p)) return ResponseEntity.notFound().build();
            UrlResource res = new UrlResource(p.toUri());

            // Whitelist erlaubter MIME-Types für Inline-Auslieferung.
            // SVG ist bewusst nicht dabei (kann <script> enthalten -> Stored XSS).
            // Bei allem anderen erzwingen wir attachment + octet-stream + nosniff.
            String storedMime = beleg.getMimeType() != null
                    ? beleg.getMimeType().toLowerCase(java.util.Locale.ROOT) : null;
            boolean inlineSicher = storedMime != null && (
                    storedMime.equals("image/jpeg") || storedMime.equals("image/jpg")
                 || storedMime.equals("image/png")  || storedMime.equals("image/webp")
                 || storedMime.equals("image/heic") || storedMime.equals("image/heif")
                 || storedMime.equals("application/pdf"));

            String contentType = inlineSicher ? storedMime : "application/octet-stream";
            String filename = beleg.getOriginalDateiname() != null
                    ? beleg.getOriginalDateiname() : ("beleg-" + id);
            // ContentDisposition.builder kümmert sich um RFC5987-Encoding der filename*=
            // Header — schützt vor CR/LF-Injection via Dateiname.
            ContentDisposition cd = (inlineSicher
                    ? ContentDisposition.inline()
                    : ContentDisposition.attachment())
                    .filename(filename, java.nio.charset.StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(res);
        } catch (Exception e) {
            log.error("Beleg-Download fehlgeschlagen", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ===================== Validierung / Löschen =====================

    @PutMapping("/belege/{id}")
    public ResponseEntity<?> updateBeleg(
            @PathVariable Long id,
            @RequestBody BelegDto.UpdateRequest req,
            @RequestParam(value = "token", required = false) String token,
            Authentication auth) {
        Mitarbeiter caller = resolveCaller(token, auth);
        // Validierung setzt Status=VALIDIERT inkl. validiertVon -> GoBD-relevante Mutation.
        // Konsistent zu DELETE: nur darfScannen (Read-only-Konten wie Steuerberater bleiben raus).
        if (caller == null || !belegService.darfScannen(caller)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        BelegDto.Response r = belegService.updateBeleg(id, req, caller);
        return r == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(r);
    }

    @DeleteMapping("/belege/{id}")
    public ResponseEntity<?> deleteBeleg(
            @PathVariable Long id,
            @RequestParam(value = "token", required = false) String token,
            Authentication auth) {
        Mitarbeiter caller = resolveCaller(token, auth);
        // Verwerfen ist eine destruktive Aktion — darf nur, wer auch Belege validieren darf
        // (darfScannen). Read-only-Konten wie Steuerberater-Accounts duerfen nicht loeschen.
        if (caller == null || !belegService.darfScannen(caller)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        boolean ok = belegService.deleteBeleg(id);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // ===================== Kassenbuch =====================

    @GetMapping("/kassenbuch")
    public ResponseEntity<?> kassenbuch(
            @RequestParam(value = "von", required = false) String von,
            @RequestParam(value = "bis", required = false) String bis,
            @RequestParam(value = "token", required = false) String token,
            Authentication auth) {
        Mitarbeiter caller = resolveCaller(token, auth);
        if (caller == null || !belegService.darfSehen(caller)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        LocalDate vonDate = parseDate(von);
        LocalDate bisDate = parseDate(bis);
        return ResponseEntity.ok(belegService.getKassenbuch(vonDate, bisDate));
    }

    // ===================== Helpers =====================

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}
