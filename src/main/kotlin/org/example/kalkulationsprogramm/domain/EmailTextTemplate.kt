package org.example.kalkulationsprogramm.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "email_text_template")
open class EmailTextTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @Column(name = "dokument_typ", nullable = false, length = 40)
    open var dokumentTyp: String? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "kategorie", length = 20)
    open var kategorie: EmailTextTemplateKategorie? = null

    @Column(name = "name", nullable = false, length = 150)
    open var name: String? = null

    @Column(name = "subject_template", nullable = false, length = 500)
    open var subjectTemplate: String? = null

    @Column(name = "html_body", nullable = false, columnDefinition = "longtext")
    open var htmlBody: String? = null

    @Column(name = "aktiv", nullable = false)
    open var aktiv: Boolean = true

    @Column(name = "created_at")
    open var createdAt: OffsetDateTime? = null

    @Column(name = "updated_at")
    open var updatedAt: OffsetDateTime? = null

    open fun isAktiv(): Boolean = aktiv

}
