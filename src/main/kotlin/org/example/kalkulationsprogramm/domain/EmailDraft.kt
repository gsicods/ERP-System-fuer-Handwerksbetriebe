package org.example.kalkulationsprogramm.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "email_draft")
open class EmailDraft {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null

    @Column(columnDefinition = "TEXT")
    open var recipient: String? = null

    @Column(columnDefinition = "TEXT")
    open var cc: String? = null

    @Column(columnDefinition = "TEXT")
    open var subject: String? = null

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    open var body: String? = null

    @Column(length = 255)
    open var fromAddress: String? = null

    open var replyEmailId: Long? = null

    open var projektId: Long? = null

    open var anfrageId: Long? = null

    open var createdAt: LocalDateTime? = null

    open var updatedAt: LocalDateTime? = null

}
