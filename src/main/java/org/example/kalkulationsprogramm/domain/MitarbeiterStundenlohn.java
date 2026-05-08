package org.example.kalkulationsprogramm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "mitarbeiter_stundenlohn")
public class MitarbeiterStundenlohn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mitarbeiter_id", nullable = false)
    private Mitarbeiter mitarbeiter;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal stundenlohn;

    @Column(name = "gueltig_ab", nullable = false)
    private LocalDate gueltigAb;

    @Column(length = 500)
    private String bemerkung;
}
