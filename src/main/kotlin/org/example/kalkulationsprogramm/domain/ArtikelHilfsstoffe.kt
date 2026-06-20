package org.example.kalkulationsprogramm.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity

@Entity
class ArtikelHilfsstoffe : Artikel() {
    @Column(name = "masse_pro_meter")
    var anzugskraefte: Long? = null
}
