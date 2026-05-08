package org.example.kalkulationsprogramm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "krankenkasse")
public class Krankenkasse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 32)
    private String kuerzel;

    @Column(name = "zusatzbeitrag_prozent", nullable = false, precision = 5, scale = 2)
    private BigDecimal zusatzbeitragProzent;

    @Column(nullable = false)
    private Boolean aktiv = true;

    @Column(name = "gueltig_ab")
    private LocalDate gueltigAb;

    @Column(length = 500)
    private String bemerkung;
}
