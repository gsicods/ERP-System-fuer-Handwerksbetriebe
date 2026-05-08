package org.example.kalkulationsprogramm.dto.Mitarbeiter;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class MitarbeiterStundenlohnDto {
    private Long id;
    private Long mitarbeiterId;

    @NotNull(message = "Stundenlohn ist Pflicht.")
    @DecimalMin(value = "0.00", message = "Stundenlohn darf nicht negativ sein.")
    @DecimalMax(value = "10000.00", message = "Stundenlohn ist unrealistisch hoch (max. 10.000 EUR).")
    private BigDecimal stundenlohn;

    @NotNull(message = "Gueltig-ab-Datum ist Pflicht.")
    private LocalDate gueltigAb;

    @Size(max = 500, message = "Bemerkung ist zu lang (max. 500 Zeichen).")
    private String bemerkung;
}
