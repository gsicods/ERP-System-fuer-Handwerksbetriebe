package org.example.kalkulationsprogramm.repository

import java.util.Optional
import org.example.kalkulationsprogramm.domain.Firmeninformation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FirmeninformationRepository : JpaRepository<Firmeninformation, Long> {
    fun findFirmeninformation(): Optional<Firmeninformation> = findById(1L)

    fun getOrCreate(): Firmeninformation =
        findById(1L).orElseGet {
            Firmeninformation().also {
                it.setLongProperty("setId", 1L)
                it.setStringProperty("setFirmenname", "Neue Firma")
            }.let { save(it) }
        }

    private fun Firmeninformation.setLongProperty(methodName: String, value: Long) {
        javaClass.getMethod(methodName, Long::class.javaObjectType).invoke(this, value)
    }

    private fun Firmeninformation.setStringProperty(methodName: String, value: String) {
        javaClass.getMethod(methodName, String::class.java).invoke(this, value)
    }
}
