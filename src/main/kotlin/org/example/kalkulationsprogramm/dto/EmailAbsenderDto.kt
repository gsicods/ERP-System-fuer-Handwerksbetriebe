package org.example.kalkulationsprogramm.dto

import org.example.kalkulationsprogramm.domain.EmailAbsender
import java.lang.reflect.Field

data class EmailAbsenderDto(
    var id: Long? = null,
    var emailAdresse: String? = null,
    var anzeigename: String? = null,
    var isAktiv: Boolean = true,
    var sortierung: Int = 0,
) {
    companion object {
        @JvmStatic
        fun fromEntity(entity: EmailAbsender?): EmailAbsenderDto? {
            if (entity == null) {
                return null
            }
            val dto = EmailAbsenderDto()
            dto.id = entity.readField("id") as? Long
            dto.emailAdresse = entity.readField("emailAdresse") as? String
            dto.anzeigename = entity.readField("anzeigename") as? String
            dto.isAktiv = entity.readField("aktiv") as? Boolean ?: true
            dto.sortierung = entity.readField("sortierung") as? Int ?: 0
            return dto
        }

        private fun EmailAbsender.readField(name: String): Any? {
            val field: Field = EmailAbsender::class.java.getDeclaredField(name)
            field.isAccessible = true
            return field.get(this)
        }
    }
}
