package org.example.kalkulationsprogramm.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.example.kalkulationsprogramm.domain.DokumentLock;
import org.example.kalkulationsprogramm.dto.DokumentLockDto;
import org.example.kalkulationsprogramm.repository.DokumentLockRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * Verwaltet Soft-Locks fuer Geschaeftsdokumente im Editor.
 *
 * Garantiert: Pro (dokumentTyp, dokumentId)-Paar haelt zu jedem Zeitpunkt
 * hoechstens ein User das Lock. Ein verwaistes Lock (Browser-Crash etc.)
 * darf nach STALE_AFTER von einem anderen User uebernommen werden.
 */
@Service
@RequiredArgsConstructor
public class DokumentLockService {

    public static final String TYP_AUSGANG = "AUSGANG";
    public static final String TYP_EINGANG = "EINGANG";

    private static final Set<String> ERLAUBTE_TYPEN = Set.of(TYP_AUSGANG, TYP_EINGANG);

    /** Lock gilt als verwaist, wenn so lange kein Heartbeat mehr kam. */
    static final Duration STALE_AFTER = Duration.ofSeconds(90);

    private final DokumentLockRepository repository;

    /**
     * Versucht, das Lock fuer den User zu erwerben.
     * Erfolg, wenn entweder kein Lock existiert, der User selbst das Lock haelt
     * oder das bestehende Lock verwaist ist.
     */
    @Transactional
    public DokumentLockDto acquire(String dokumentTyp, Long dokumentId, Long userId, String userDisplayName) {
        validateTyp(dokumentTyp);
        LocalDateTime now = LocalDateTime.now();

        Optional<DokumentLock> existing = repository.findByDokumentTypAndDokumentId(dokumentTyp, dokumentId);
        if (existing.isPresent()) {
            DokumentLock lock = existing.get();
            boolean sameUser = lock.getUserId().equals(userId);
            boolean stale = isStale(lock, now);
            if (sameUser || stale) {
                lock.setUserId(userId);
                lock.setUserDisplayName(safeDisplayName(userDisplayName));
                if (!sameUser) {
                    lock.setAcquiredAt(now);
                }
                lock.setLastHeartbeatAt(now);
                DokumentLock saved = repository.save(lock);
                return acquired(saved);
            }
            return lockedByOther(lock);
        }

        DokumentLock fresh = new DokumentLock();
        fresh.setDokumentTyp(dokumentTyp);
        fresh.setDokumentId(dokumentId);
        fresh.setUserId(userId);
        fresh.setUserDisplayName(safeDisplayName(userDisplayName));
        fresh.setAcquiredAt(now);
        fresh.setLastHeartbeatAt(now);
        try {
            DokumentLock saved = repository.saveAndFlush(fresh);
            return acquired(saved);
        } catch (DataIntegrityViolationException race) {
            // Konkurrierender Insert hat zwischen findBy und save den Lock geschrieben.
            // Den jetzt sichtbaren Eintrag wieder pruefen.
            DokumentLock winner = repository.findByDokumentTypAndDokumentId(dokumentTyp, dokumentId)
                    .orElseThrow(() -> race);
            if (winner.getUserId().equals(userId)) {
                return acquired(winner);
            }
            return lockedByOther(winner);
        }
    }

    /**
     * Verlaengert das Lock des Users. Liefert ACQUIRED nur, wenn der Caller
     * tatsaechlich noch Owner ist; sonst LOCKED_BY_OTHER, damit der Frontend
     * den Editor schliessen kann.
     */
    @Transactional
    public DokumentLockDto heartbeat(String dokumentTyp, Long dokumentId, Long userId, String userDisplayName) {
        validateTyp(dokumentTyp);
        Optional<DokumentLock> existing = repository.findByDokumentTypAndDokumentId(dokumentTyp, dokumentId);
        if (existing.isEmpty()) {
            // Lock ist weg (Cleanup oder anderer Tab) — neu erwerben statt 404.
            return acquire(dokumentTyp, dokumentId, userId, userDisplayName);
        }
        DokumentLock lock = existing.get();
        if (!lock.getUserId().equals(userId)) {
            if (isStale(lock, LocalDateTime.now())) {
                return acquire(dokumentTyp, dokumentId, userId, userDisplayName);
            }
            return lockedByOther(lock);
        }
        lock.setLastHeartbeatAt(LocalDateTime.now());
        if (userDisplayName != null && !userDisplayName.isBlank()) {
            lock.setUserDisplayName(userDisplayName);
        }
        return acquired(repository.save(lock));
    }

    /**
     * Gibt das Lock frei. No-op, wenn der Eintrag bereits weg ist oder einem
     * anderen User gehoert (z.B. weil ein verwaistes Lock zwischenzeitlich
     * uebernommen wurde).
     */
    @Transactional
    public void release(String dokumentTyp, Long dokumentId, Long userId) {
        validateTyp(dokumentTyp);
        repository.findByDokumentTypAndDokumentId(dokumentTyp, dokumentId)
                .filter(lock -> lock.getUserId().equals(userId))
                .ifPresent(repository::delete);
    }

    /**
     * Prueft, ob der User aktuell der Lock-Halter ist. Wird vor dem Speichern
     * verwendet, damit niemand am Lock vorbei schreibt.
     */
    @Transactional(readOnly = true)
    public boolean isHeldBy(String dokumentTyp, Long dokumentId, Long userId) {
        validateTyp(dokumentTyp);
        return repository.findByDokumentTypAndDokumentId(dokumentTyp, dokumentId)
                .map(lock -> lock.getUserId().equals(userId) && !isStale(lock, LocalDateTime.now()))
                .orElse(false);
    }

    private boolean isStale(DokumentLock lock, LocalDateTime now) {
        return Duration.between(lock.getLastHeartbeatAt(), now).compareTo(STALE_AFTER) > 0;
    }

    private void validateTyp(String dokumentTyp) {
        if (dokumentTyp == null || !ERLAUBTE_TYPEN.contains(dokumentTyp)) {
            throw new IllegalArgumentException("Unbekannter dokumentTyp: " + dokumentTyp);
        }
    }

    private String safeDisplayName(String userDisplayName) {
        if (userDisplayName == null || userDisplayName.isBlank()) {
            return "Unbekannter Benutzer";
        }
        return userDisplayName.length() > 255 ? userDisplayName.substring(0, 255) : userDisplayName;
    }

    private DokumentLockDto acquired(DokumentLock lock) {
        return new DokumentLockDto(
                DokumentLockDto.ACQUIRED,
                lock.getUserId(),
                lock.getUserDisplayName(),
                lock.getAcquiredAt(),
                lock.getLastHeartbeatAt()
        );
    }

    private DokumentLockDto lockedByOther(DokumentLock lock) {
        return new DokumentLockDto(
                DokumentLockDto.LOCKED_BY_OTHER,
                lock.getUserId(),
                lock.getUserDisplayName(),
                lock.getAcquiredAt(),
                lock.getLastHeartbeatAt()
        );
    }
}
