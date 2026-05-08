package org.example.kalkulationsprogramm.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.example.kalkulationsprogramm.domain.DokumentLock;
import org.example.kalkulationsprogramm.dto.DokumentLockDto;
import org.example.kalkulationsprogramm.repository.DokumentLockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DokumentLockServiceTest {

    private static final String TYP = DokumentLockService.TYP_AUSGANG;
    private static final long DOC_ID = 42L;
    private static final long USER_A = 1L;
    private static final long USER_B = 2L;

    private DokumentLockRepository repository;
    private DokumentLockService service;

    @BeforeEach
    void setUp() {
        repository = mock(DokumentLockRepository.class);
        service = new DokumentLockService(repository);
    }

    @Test
    void acquire_freshLock_returnsAcquiredAndPersistsEntry() {
        when(repository.findByDokumentTypAndDokumentId(TYP, DOC_ID)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(DokumentLock.class))).thenAnswer(inv -> inv.getArgument(0));

        DokumentLockDto result = service.acquire(TYP, DOC_ID, USER_A, "Max Mustermann");

        assertThat(result.status()).isEqualTo(DokumentLockDto.ACQUIRED);
        assertThat(result.holderUserId()).isEqualTo(USER_A);
        assertThat(result.holderDisplayName()).isEqualTo("Max Mustermann");
        verify(repository).saveAndFlush(any(DokumentLock.class));
    }

    @Test
    void acquire_existingLockSameUser_renewsAndReturnsAcquired() {
        DokumentLock existing = lockHeldBy(USER_A, "Max Mustermann", LocalDateTime.now().minusSeconds(20));
        when(repository.findByDokumentTypAndDokumentId(TYP, DOC_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(DokumentLock.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDateTime originalAcquiredAt = existing.getAcquiredAt();
        DokumentLockDto result = service.acquire(TYP, DOC_ID, USER_A, "Max Mustermann");

        assertThat(result.status()).isEqualTo(DokumentLockDto.ACQUIRED);
        // Bei demselben User darf der ursprüngliche acquiredAt-Zeitpunkt nicht
        // ueberschrieben werden — nur lastHeartbeatAt aktualisiert sich.
        assertThat(existing.getAcquiredAt()).isEqualTo(originalAcquiredAt);
    }

    @Test
    void acquire_lockHeldByOtherButStale_takesOver() {
        LocalDateTime longAgo = LocalDateTime.now().minus(DokumentLockService.STALE_AFTER).minusSeconds(10);
        DokumentLock stale = lockHeldBy(USER_B, "Erika Mustermann", longAgo);
        when(repository.findByDokumentTypAndDokumentId(TYP, DOC_ID)).thenReturn(Optional.of(stale));
        when(repository.save(any(DokumentLock.class))).thenAnswer(inv -> inv.getArgument(0));

        DokumentLockDto result = service.acquire(TYP, DOC_ID, USER_A, "Max Mustermann");

        assertThat(result.status()).isEqualTo(DokumentLockDto.ACQUIRED);
        assertThat(result.holderUserId()).isEqualTo(USER_A);
        assertThat(stale.getUserId()).isEqualTo(USER_A);
        // Bei Takeover wird auch acquiredAt frisch gesetzt.
        assertThat(stale.getAcquiredAt()).isAfter(longAgo);
    }

    @Test
    void acquire_lockHeldByOtherFresh_returnsLockedByOther() {
        DokumentLock fresh = lockHeldBy(USER_B, "Erika Mustermann", LocalDateTime.now().minusSeconds(5));
        when(repository.findByDokumentTypAndDokumentId(TYP, DOC_ID)).thenReturn(Optional.of(fresh));

        DokumentLockDto result = service.acquire(TYP, DOC_ID, USER_A, "Max Mustermann");

        assertThat(result.status()).isEqualTo(DokumentLockDto.LOCKED_BY_OTHER);
        assertThat(result.holderUserId()).isEqualTo(USER_B);
        assertThat(result.holderDisplayName()).isEqualTo("Erika Mustermann");
        verify(repository, never()).save(any(DokumentLock.class));
        verify(repository, never()).saveAndFlush(any(DokumentLock.class));
    }

    @Test
    void acquire_concurrentInsertRace_returnsLockedByOtherForLoser() {
        // Erster findBy liefert nichts (Race-Setup), saveAndFlush schlaegt am
        // Unique-Constraint fehl, danach liefert findBy den Gewinner zurueck.
        DokumentLock winner = lockHeldBy(USER_B, "Erika Mustermann", LocalDateTime.now());
        when(repository.findByDokumentTypAndDokumentId(TYP, DOC_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(repository.saveAndFlush(any(DokumentLock.class)))
                .thenThrow(new DataIntegrityViolationException("uk_dokument_lock_target"));

        DokumentLockDto result = service.acquire(TYP, DOC_ID, USER_A, "Max Mustermann");

        assertThat(result.status()).isEqualTo(DokumentLockDto.LOCKED_BY_OTHER);
        assertThat(result.holderUserId()).isEqualTo(USER_B);
    }

    @Test
    void heartbeat_byOwner_extendsAndStaysAcquired() {
        DokumentLock existing = lockHeldBy(USER_A, "Max Mustermann", LocalDateTime.now().minusSeconds(25));
        when(repository.findByDokumentTypAndDokumentId(TYP, DOC_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(DokumentLock.class))).thenAnswer(inv -> inv.getArgument(0));

        DokumentLockDto result = service.heartbeat(TYP, DOC_ID, USER_A, "Max Mustermann");

        assertThat(result.status()).isEqualTo(DokumentLockDto.ACQUIRED);
        verify(repository).save(any(DokumentLock.class));
    }

    @Test
    void heartbeat_byNonOwnerWithFreshLock_returnsLockedByOther() {
        DokumentLock fresh = lockHeldBy(USER_B, "Erika Mustermann", LocalDateTime.now().minusSeconds(5));
        when(repository.findByDokumentTypAndDokumentId(TYP, DOC_ID)).thenReturn(Optional.of(fresh));

        DokumentLockDto result = service.heartbeat(TYP, DOC_ID, USER_A, "Max Mustermann");

        assertThat(result.status()).isEqualTo(DokumentLockDto.LOCKED_BY_OTHER);
        assertThat(result.holderUserId()).isEqualTo(USER_B);
    }

    @Test
    void heartbeat_byNonOwnerOfStaleLock_takesOver() {
        LocalDateTime longAgo = LocalDateTime.now().minus(DokumentLockService.STALE_AFTER).minusSeconds(30);
        DokumentLock stale = lockHeldBy(USER_B, "Erika Mustermann", longAgo);
        when(repository.findByDokumentTypAndDokumentId(TYP, DOC_ID)).thenReturn(Optional.of(stale));
        when(repository.save(any(DokumentLock.class))).thenAnswer(inv -> inv.getArgument(0));

        DokumentLockDto result = service.heartbeat(TYP, DOC_ID, USER_A, "Max Mustermann");

        assertThat(result.status()).isEqualTo(DokumentLockDto.ACQUIRED);
        assertThat(result.holderUserId()).isEqualTo(USER_A);
    }

    @Test
    void heartbeat_lockMissing_acquiresFresh() {
        when(repository.findByDokumentTypAndDokumentId(TYP, DOC_ID)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(DokumentLock.class))).thenAnswer(inv -> inv.getArgument(0));

        DokumentLockDto result = service.heartbeat(TYP, DOC_ID, USER_A, "Max Mustermann");

        assertThat(result.status()).isEqualTo(DokumentLockDto.ACQUIRED);
    }

    @Test
    void release_byOwner_deletesLock() {
        DokumentLock existing = lockHeldBy(USER_A, "Max Mustermann", LocalDateTime.now());
        when(repository.findByDokumentTypAndDokumentId(TYP, DOC_ID)).thenReturn(Optional.of(existing));

        service.release(TYP, DOC_ID, USER_A);

        verify(repository, times(1)).delete(existing);
    }

    @Test
    void release_byNonOwner_keepsLock() {
        DokumentLock existing = lockHeldBy(USER_B, "Erika Mustermann", LocalDateTime.now());
        when(repository.findByDokumentTypAndDokumentId(TYP, DOC_ID)).thenReturn(Optional.of(existing));

        service.release(TYP, DOC_ID, USER_A);

        verify(repository, never()).delete(any(DokumentLock.class));
    }

    @Test
    void isHeldBy_returnsTrueOnlyForOwnerWithFreshHeartbeat() {
        DokumentLock fresh = lockHeldBy(USER_A, "Max Mustermann", LocalDateTime.now().minusSeconds(5));
        when(repository.findByDokumentTypAndDokumentId(TYP, DOC_ID)).thenReturn(Optional.of(fresh));

        assertThat(service.isHeldBy(TYP, DOC_ID, USER_A)).isTrue();
        assertThat(service.isHeldBy(TYP, DOC_ID, USER_B)).isFalse();
    }

    @Test
    void isHeldBy_staleLock_returnsFalseEvenForOwner() {
        LocalDateTime longAgo = LocalDateTime.now().minus(DokumentLockService.STALE_AFTER).minusSeconds(10);
        DokumentLock stale = lockHeldBy(USER_A, "Max Mustermann", longAgo);
        when(repository.findByDokumentTypAndDokumentId(TYP, DOC_ID)).thenReturn(Optional.of(stale));

        assertThat(service.isHeldBy(TYP, DOC_ID, USER_A)).isFalse();
    }

    @Test
    void isHeldBy_noLockEntry_returnsFalse() {
        when(repository.findByDokumentTypAndDokumentId(TYP, DOC_ID)).thenReturn(Optional.empty());

        assertThat(service.isHeldBy(TYP, DOC_ID, USER_A)).isFalse();
    }

    @Test
    void invalidTyp_throws() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.acquire("UNBEKANNT", DOC_ID, USER_A, "Max Mustermann"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private DokumentLock lockHeldBy(long userId, String displayName, LocalDateTime lastHeartbeat) {
        DokumentLock lock = new DokumentLock();
        lock.setId(7L);
        lock.setDokumentTyp(TYP);
        lock.setDokumentId(DOC_ID);
        lock.setUserId(userId);
        lock.setUserDisplayName(displayName);
        lock.setAcquiredAt(lastHeartbeat);
        lock.setLastHeartbeatAt(lastHeartbeat);
        return lock;
    }
}
