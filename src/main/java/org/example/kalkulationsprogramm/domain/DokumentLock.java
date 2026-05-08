package org.example.kalkulationsprogramm.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Soft-Lock fuer Geschaeftsdokumente im Editor.
 * Genau ein User darf ein (dokumentTyp, dokumentId)-Paar gleichzeitig
 * geoeffnet halten. Der Eintrag wird per Heartbeat am Leben gehalten und
 * nach 90s Heartbeat-Stille als verwaist behandelt.
 */
@Getter
@Setter
@Entity
@Table(
    name = "dokument_lock",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_dokument_lock_target",
        columnNames = {"dokument_typ", "dokument_id"}
    )
)
public class DokumentLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dokument_typ", nullable = false, length = 16)
    private String dokumentTyp;

    @Column(name = "dokument_id", nullable = false)
    private Long dokumentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_display_name", nullable = false, length = 255)
    private String userDisplayName;

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

    @Column(name = "last_heartbeat_at", nullable = false)
    private LocalDateTime lastHeartbeatAt;
}
