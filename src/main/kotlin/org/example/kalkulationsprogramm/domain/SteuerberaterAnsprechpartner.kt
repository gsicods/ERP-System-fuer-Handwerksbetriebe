package org.example.kalkulationsprogramm.domain

import com.fasterxml.jackson.annotation.JsonBackReference
import jakarta.persistence.*

@Entity
@Table(name = "steuerberater_ansprechpartner")
open class SteuerberaterAnsprechpartner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "steuerberater_id", nullable = false)
    @JsonBackReference
    open var steuerberater: SteuerberaterKontakt? = null

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    open var anrede: Anrede? = null

    open var vorname: String? = null

    @Column(nullable = false)
    open var nachname: String? = null

    open var email: String? = null

    open var telefon: String? = null

    @Column(nullable = false)
    open var istLohnAnsprechpartner: Boolean? = false

    @Column(length = 500)
    open var notizen: String? = null

}
