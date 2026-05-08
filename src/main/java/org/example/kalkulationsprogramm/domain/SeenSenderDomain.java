package org.example.kalkulationsprogramm.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Erstkontakt-Tracking fuer den Spam-Filter.
 *
 * Pro Absender-Domain ein Eintrag mit dem Zeitpunkt der ersten beobachteten
 * Mail. Eine "neue" Domain in Kombination mit Free-Mailer + mehreren Links
 * ist ein typisches Cold-Mail-Signal.
 */
@Entity
@Table(name = "seen_sender_domain")
@Getter
@Setter
@NoArgsConstructor
public class SeenSenderDomain {

    @Id
    @Column(nullable = false, length = 255)
    private String domain;

    @Column(name = "first_seen", nullable = false)
    private LocalDateTime firstSeen;

    @Column(name = "email_count", nullable = false)
    private int emailCount = 1;
}
