package org.example.kalkulationsprogramm.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.time.LocalDateTime

@Entity
@Table(name = "entity_last_accessed")
open class EntityLastAccessed {
    @EmbeddedId
    open var id: EntityLastAccessedId? = null

    @Column(name = "zugegriffen_am", nullable = false)
    open var zugegriffenAm: LocalDateTime? = null

    constructor()

    constructor(userId: Long?, entityType: String?, entityId: Long?, zugegriffenAm: LocalDateTime?) {
        this.id = EntityLastAccessedId(userId, entityType, entityId)
        this.zugegriffenAm = zugegriffenAm
    }

    @Embeddable
    open class EntityLastAccessedId : Serializable {
        @Column(name = "user_id", nullable = false)
        open var userId: Long? = null

        @Column(name = "entity_type", nullable = false, length = 64)
        open var entityType: String? = null

        @Column(name = "entity_id", nullable = false)
        open var entityId: Long? = null

        constructor()

        constructor(userId: Long?, entityType: String?, entityId: Long?) {
            this.userId = userId
            this.entityType = entityType
            this.entityId = entityId
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is EntityLastAccessedId) return false
            return userId == other.userId && entityType == other.entityType && entityId == other.entityId
        }

        override fun hashCode(): Int = java.util.Objects.hash(userId, entityType, entityId)
    }

}
