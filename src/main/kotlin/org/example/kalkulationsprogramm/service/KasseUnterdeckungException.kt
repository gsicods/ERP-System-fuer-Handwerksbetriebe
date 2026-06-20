package org.example.kalkulationsprogramm.service

import java.math.BigDecimal

class KasseUnterdeckungException(
    val projizierterSaldo: BigDecimal,
    val mindestbestand: BigDecimal,
) : RuntimeException("Kasse wuerde auf $projizierterSaldo EUR rutschen (Mindestbestand: $mindestbestand EUR). Bitte vorher Bank-Abhebung oder Privateinlage buchen.")
