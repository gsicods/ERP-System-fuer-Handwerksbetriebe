package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "email_signature")
open class EmailSignature {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @Column(nullable = false, length = 200)
    open var name: String? = null

    @Lob
    @Column(name = "html", nullable = false, columnDefinition = "longtext")
    open var html: String? = null

    @Transient
    open var defaultSignature: Boolean = false

    open fun isDefaultSignature(): Boolean = defaultSignature

    @Column(name = "is_system_default", nullable = false)
    open var isSystemDefault: Boolean = false

    @Column(name = "created_at", nullable = false)
    open var createdAt: LocalDateTime? = null

    @Column(name = "updated_at", nullable = false)
    open var updatedAt: LocalDateTime? = null

    @com.fasterxml.jackson.annotation.JsonManagedReference
    @OneToMany(mappedBy = "signature", cascade = arrayOf(CascadeType.ALL), orphanRemoval = true, fetch = FetchType.LAZY)
    open var images: MutableList<EmailSignatureImage> = mutableListOf()

}
