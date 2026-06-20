package org.example.kalkulationsprogramm.event

data class EmailAddressChangedEvent(
    val entityType: EntityType,
    val entityId: Long,
    val newAddresses: List<String>,
    val allAddresses: List<String>,
    val newEntity: Boolean
) {
    enum class EntityType {
        KUNDE,
        LIEFERANT,
        ANFRAGE,
        PROJEKT,
        ANGEBOT
    }

    fun isNewEntity(): Boolean = newEntity

    companion object {
        @JvmStatic
        fun forNewEntity(type: EntityType, id: Long, addresses: List<String>): EmailAddressChangedEvent =
            EmailAddressChangedEvent(type, id, addresses, addresses, true)

        @JvmStatic
        fun forAddressChange(
            type: EntityType,
            id: Long,
            newAddresses: List<String>,
            allAddresses: List<String>
        ): EmailAddressChangedEvent =
            EmailAddressChangedEvent(type, id, newAddresses, allAddresses, false)
    }
}
