package org.example.kalkulationsprogramm.domain;

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
 * Konfigurierbare Absender-E-Mail-Adresse.
 * Wird im FirmaEditor gepflegt und kann einzelnen FrontendUserProfile-
 * Eintraegen zugewiesen werden. Beim Versand einer E-Mail wird die zum
 * eingeloggten Benutzer gehoerende Adresse als "From" verwendet.
 */
@Getter
@Setter
@Entity
@Table(
        name = "email_absender",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_email_absender_adresse", columnNames = "email_adresse")
        }
)
public class EmailAbsender {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_adresse", nullable = false, length = 255)
    private String emailAdresse;

    @Column(name = "anzeigename", length = 255)
    private String anzeigename;

    @Column(name = "aktiv", nullable = false)
    private boolean aktiv = true;

    @Column(name = "sortierung", nullable = false)
    private int sortierung = 0;
}
