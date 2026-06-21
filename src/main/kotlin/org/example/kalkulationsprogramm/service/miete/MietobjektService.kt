package org.example.kalkulationsprogramm.service.miete

import org.example.kalkulationsprogramm.domain.miete.Mietobjekt
import org.example.kalkulationsprogramm.domain.miete.Mietpartei
import org.example.kalkulationsprogramm.domain.miete.MietparteiRolle
import org.example.kalkulationsprogramm.exception.NotFoundException
import org.example.kalkulationsprogramm.repository.miete.MietobjektRepository
import org.example.kalkulationsprogramm.repository.miete.MietparteiRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
@Transactional
class MietobjektService(
    private val mietobjektRepository: MietobjektRepository,
    private val mietparteiRepository: MietparteiRepository
) {
    fun findAll(): List<Mietobjekt> = mietobjektRepository.findAll()

    fun getById(id: Long): Mietobjekt =
        mietobjektRepository.findById(id).orElseThrow { NotFoundException("Mietobjekt $id nicht gefunden") }

    fun save(mietobjekt: Mietobjekt): Mietobjekt = mietobjektRepository.save(mietobjekt)

    fun delete(id: Long) {
        mietobjektRepository.delete(getById(id))
    }

    fun savePartei(mietobjektId: Long, partei: Mietpartei): Mietpartei {
        val mietobjekt = getById(mietobjektId)
        partei.mietobjekt = mietobjekt
        if (partei.rolle != MietparteiRolle.MIETER) {
            partei.monatlicherVorschuss = null
        } else {
            val vorschuss = partei.monatlicherVorschuss
            if (vorschuss == null) return mietparteiRepository.save(partei)
            val value = vorschuss.max(BigDecimal.ZERO)
            partei.monatlicherVorschuss = value.setScale(2, RoundingMode.HALF_UP)
        }
        return mietparteiRepository.save(partei)
    }

    fun deletePartei(parteiId: Long) {
        val partei = mietparteiRepository.findById(parteiId)
            .orElseThrow { NotFoundException("Mietpartei $parteiId nicht gefunden") }
        mietparteiRepository.delete(partei)
    }

    fun getParteien(mietobjektId: Long): List<Mietpartei> =
        mietparteiRepository.findByMietobjektOrderByNameAsc(getById(mietobjektId))
}
