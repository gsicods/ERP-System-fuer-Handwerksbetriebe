package org.example.kalkulationsprogramm.domain

import jakarta.persistence.*

@Entity
@Table(name = "email_signature_image")
open class EmailSignatureImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @com.fasterxml.jackson.annotation.JsonBackReference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signature_id", nullable = false)
    open var signature: EmailSignature? = null

    @Column(name = "cid", nullable = false, length = 120)
    open var cid: String? = null

    @Column(name = "original_filename", nullable = false)
    open var originalFilename: String? = null

    @Column(name = "stored_filename", nullable = false)
    open var storedFilename: String? = null

    @Column(name = "content_type", nullable = false)
    open var contentType: String? = null

    @Column(name = "size_bytes", nullable = false)
    open var sizeBytes: Long? = null

    @Column(name = "sort_order")
    open var sortOrder: Int = 0

}
