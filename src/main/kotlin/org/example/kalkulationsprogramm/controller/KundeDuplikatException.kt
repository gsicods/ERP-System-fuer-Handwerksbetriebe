package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.dto.Kunde.KundeDuplikatResponseDto

class KundeDuplikatException(val antwort: KundeDuplikatResponseDto) : RuntimeException("Moeglicher Kunden-Duplikat erkannt.")
