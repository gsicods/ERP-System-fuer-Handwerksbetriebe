package org.example.kalkulationsprogramm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class KrankenkasseDto {
    private Long id;

    @NotBlank(message = "Name darf nicht leer sein.")
    @Size(max = 255, message = "Name ist zu lang (max. 255 Zeichen).")
    private String name;

    @Size(max = 32, message = "Kuerzel ist zu lang (max. 32 Zeichen).")
    private String kuerzel;

    @NotNull(message = "Zusatzbeitrag (Prozent) ist Pflicht.")
    @DecimalMin(value = "0.00", message = "Zusatzbeitrag darf nicht negativ sein.")
    @DecimalMax(value = "100.00", message = "Zusatzbeitrag darf nicht ueber 100 % liegen.")
    private BigDecimal zusatzbeitragProzent;

    private Boolean aktiv;
    private LocalDate gueltigAb;

    @Size(max = 500, message = "Bemerkung ist zu lang (max. 500 Zeichen).")
    private String bemerkung;
}
