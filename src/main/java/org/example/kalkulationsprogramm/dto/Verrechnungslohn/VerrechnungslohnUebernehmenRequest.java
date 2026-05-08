package org.example.kalkulationsprogramm.dto.Verrechnungslohn;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Request fuer das globale Uebernehmen des berechneten Verrechnungslohns
 * auf alle Arbeitsgaenge eines Jahres.
 *
 * basisSatz   - Selbstkosten pro Stunde × (1 + Gewinn-%) - der einheitliche
 *               Verkaufspreis pro Stunde.
 * abteilungAufschlaege - optionale Per-Abteilung-Korrekturen (positiv/negativ),
 *               werden auf basisSatz aufgeschlagen.
 */
@Getter
@Setter
public class VerrechnungslohnUebernehmenRequest {

    @Min(2000)
    @Max(2100)
    private int jahr;

    @NotNull
    @DecimalMin(value = "0.01", message = "basisSatz muss positiv sein")
    private BigDecimal basisSatz;

    private List<AbteilungAufschlag> abteilungAufschlaege = new ArrayList<>();

    @Getter
    @Setter
    public static class AbteilungAufschlag {
        private Long abteilungId;
        private BigDecimal aufschlagEuro = BigDecimal.ZERO;
    }
}
