package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "textbaustein")
open class Textbaustein {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @Column(nullable = false, length = 150)
    open var name: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "typ", nullable = false, length = 40)
    open var typ: TextbausteinTyp? = TextbausteinTyp.FREITEXT

    @Column(name = "beschreibung", length = 500)
    open var beschreibung: String? = null

    @Column(name = "html", columnDefinition = "longtext")
    open var html: String? = null

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "textbaustein_placeholder")
    @Column(name = "placeholder", length = 120)
    open var placeholders: MutableSet<String> = linkedSetOf()

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "textbaustein_dokumenttyp_enum")
    @Column(name = "dokumenttyp", length = 30, columnDefinition = "varchar(30)")
    open var dokumenttypen: MutableSet<Dokumenttyp> = linkedSetOf()

    @Column(name = "sort_order")
    open var sortOrder: Int? = null

    @Column(name = "created_at")
    open var createdAt: OffsetDateTime? = null

    @Column(name = "updated_at")
    open var updatedAt: OffsetDateTime? = null

}
