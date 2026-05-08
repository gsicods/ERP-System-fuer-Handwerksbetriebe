package org.example.kalkulationsprogramm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "sv_satz")
public class SvSatz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "satz_typ", nullable = false)
    private SvSatzTyp satzTyp;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal prozent;

    @Column(name = "gueltig_ab", nullable = false)
    private LocalDate gueltigAb;

    @Column(length = 500)
    private String beschreibung;
}
