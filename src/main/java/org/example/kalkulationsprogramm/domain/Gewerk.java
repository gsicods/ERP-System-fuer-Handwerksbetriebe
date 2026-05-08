package org.example.kalkulationsprogramm.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "gewerk")
public class Gewerk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "bg_name", nullable = false)
    private String bgName;

    @Column(name = "bg_satz_prozent", nullable = false, precision = 5, scale = 2)
    private BigDecimal bgSatzProzent;

    @Column(nullable = false)
    private Boolean aktiv = true;

    @Column(length = 500)
    private String bemerkung;
}
