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
public class SvSatzDto {
    private Long id;

    @NotBlank(message = "Satz-Typ ist Pflicht.")
    private String satzTyp;

    @NotNull(message = "Prozent ist Pflicht.")
    @DecimalMin(value = "0.00", message = "Prozent darf nicht negativ sein.")
    @DecimalMax(value = "100.00", message = "Prozent darf nicht ueber 100 % liegen.")
    private BigDecimal prozent;

    @NotNull(message = "Gueltig-ab-Datum ist Pflicht.")
    private LocalDate gueltigAb;

    @Size(max = 500, message = "Beschreibung ist zu lang (max. 500 Zeichen).")
    private String beschreibung;
}
