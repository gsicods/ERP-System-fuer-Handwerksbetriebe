package org.example.kalkulationsprogramm.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class GewerkDto {
    private Long id;

    @NotBlank(message = "Name darf nicht leer sein.")
    @Size(max = 255, message = "Name ist zu lang (max. 255 Zeichen).")
    private String name;

    @NotBlank(message = "BG-Name darf nicht leer sein.")
    @Size(max = 255, message = "BG-Name ist zu lang (max. 255 Zeichen).")
    private String bgName;

    @NotNull(message = "BG-Satz (Prozent) ist Pflicht.")
    @DecimalMin(value = "0.00", message = "BG-Satz darf nicht negativ sein.")
    @DecimalMax(value = "100.00", message = "BG-Satz darf nicht ueber 100 % liegen.")
    private BigDecimal bgSatzProzent;

    private Boolean aktiv;

    @Size(max = 500, message = "Bemerkung ist zu lang (max. 500 Zeichen).")
    private String bemerkung;
}
