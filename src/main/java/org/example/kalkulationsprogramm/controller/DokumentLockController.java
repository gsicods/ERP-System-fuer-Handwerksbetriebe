package org.example.kalkulationsprogramm.controller;

import java.util.Set;

import org.example.kalkulationsprogramm.config.FrontendUserPrincipal;
import org.example.kalkulationsprogramm.dto.DokumentLockDto;
import org.example.kalkulationsprogramm.service.DokumentLockService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * Endpoints fuer das Soft-Lock im Dokumenten-Editor.
 *
 *   POST   /api/dokument-locks/{typ}/{id}/acquire    — Lock erwerben oder uebernehmen
 *   POST   /api/dokument-locks/{typ}/{id}/heartbeat  — Lock am Leben halten
 *   DELETE /api/dokument-locks/{typ}/{id}            — Lock aktiv freigeben
 *
 * typ: AUSGANG | EINGANG (Gross-/Kleinschreibung egal).
 */
@RestController
@RequestMapping("/api/dokument-locks")
@RequiredArgsConstructor
public class DokumentLockController {

    private static final Set<String> ERLAUBTE_TYPEN = Set.of(
            DokumentLockService.TYP_AUSGANG,
            DokumentLockService.TYP_EINGANG
    );

    private final DokumentLockService service;

    @PostMapping("/{dokumentTyp}/{dokumentId}/acquire")
    public ResponseEntity<DokumentLockDto> acquire(@PathVariable String dokumentTyp,
                                                    @PathVariable Long dokumentId,
                                                    Authentication authentication) {
        FrontendUserPrincipal principal = principal(authentication);
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String normalized = normalizeTyp(dokumentTyp);
        if (normalized == null) {
            return ResponseEntity.badRequest().build();
        }
        DokumentLockDto result = service.acquire(normalized, dokumentId, principal.getId(), principal.getDisplayName());
        if (DokumentLockDto.LOCKED_BY_OTHER.equals(result.status())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{dokumentTyp}/{dokumentId}/heartbeat")
    public ResponseEntity<DokumentLockDto> heartbeat(@PathVariable String dokumentTyp,
                                                      @PathVariable Long dokumentId,
                                                      Authentication authentication) {
        FrontendUserPrincipal principal = principal(authentication);
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String normalized = normalizeTyp(dokumentTyp);
        if (normalized == null) {
            return ResponseEntity.badRequest().build();
        }
        DokumentLockDto result = service.heartbeat(normalized, dokumentId, principal.getId(), principal.getDisplayName());
        if (DokumentLockDto.LOCKED_BY_OTHER.equals(result.status())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{dokumentTyp}/{dokumentId}")
    public ResponseEntity<Void> release(@PathVariable String dokumentTyp,
                                        @PathVariable Long dokumentId,
                                        Authentication authentication) {
        FrontendUserPrincipal principal = principal(authentication);
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String normalized = normalizeTyp(dokumentTyp);
        if (normalized == null) {
            return ResponseEntity.badRequest().build();
        }
        service.release(normalized, dokumentId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    private FrontendUserPrincipal principal(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        if (authentication.getPrincipal() instanceof FrontendUserPrincipal principal) {
            return principal;
        }
        return null;
    }

    private String normalizeTyp(String dokumentTyp) {
        if (dokumentTyp == null) return null;
        String upper = dokumentTyp.trim().toUpperCase();
        return ERLAUBTE_TYPEN.contains(upper) ? upper : null;
    }
}
