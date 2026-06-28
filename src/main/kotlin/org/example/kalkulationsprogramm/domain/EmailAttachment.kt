package org.example.kalkulationsprogramm.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "email_attachment",
    indexes = [
        Index(name = "idx_attachment_email", columnList = "email_id"),
        Index(name = "idx_attachment_ai", columnList = "aiProcessed"),
    ],
)
class EmailAttachment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "email_id", nullable = false)
    var email: Email? = null

    @Column(length = 500)
    var originalFilename: String? = null

    @Column(length = 500)
    var storedFilename: String? = null

    @Column(length = 255)
    var contentId: String? = null

    var inlineAttachment: Boolean? = false

    @Column(length = 255)
    var mimeType: String? = null

    var sizeBytes: Long? = null

    var aiProcessed: Boolean? = false

    var aiProcessedAt: LocalDateTime? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lieferant_dokument_id")
    var lieferantDokument: LieferantDokument? = null

    fun isPdf(): Boolean = originalFilename?.lowercase()?.endsWith(".pdf") == true

    fun isXml(): Boolean = originalFilename?.lowercase()?.endsWith(".xml") == true

    fun isImage(): Boolean {
        val type = mimeType
        if (type != null) {
            return type.startsWith("image/")
        }
        val lower = originalFilename?.lowercase() ?: return false
        return lower.endsWith(".jpg") ||
            lower.endsWith(".jpeg") ||
            lower.endsWith(".png") ||
            lower.endsWith(".gif")
    }

    fun isInline(): Boolean = inlineAttachment == true

    fun markAsAiProcessed(dokument: LieferantDokument?) {
        aiProcessed = true
        aiProcessedAt = LocalDateTime.now()
        lieferantDokument = dokument
    }
}
