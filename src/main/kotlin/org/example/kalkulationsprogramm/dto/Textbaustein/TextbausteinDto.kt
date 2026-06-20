package org.example.kalkulationsprogramm.dto.Textbaustein

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.example.kalkulationsprogramm.domain.Dokumenttyp
import org.example.kalkulationsprogramm.domain.Textbaustein
import org.example.kalkulationsprogramm.domain.TextbausteinTyp
import java.lang.reflect.Field
import java.time.OffsetDateTime
import java.util.LinkedHashSet

data class TextbausteinDto(
    var id: Long? = null,
    @field:NotBlank
    @field:Size(max = 150)
    var name: String? = null,
    @field:NotBlank
    @field:Size(max = 40)
    var typ: String? = null,
    @field:Size(max = 500)
    var beschreibung: String? = null,
    var html: String? = null,
    var placeholders: MutableList<String> = ArrayList(),
    var dokumenttypen: MutableList<String> = ArrayList(),
    var sortOrder: Int? = null,
    var createdAt: OffsetDateTime? = null,
    var updatedAt: OffsetDateTime? = null,
) {
    fun applyToEntity(entity: Textbaustein) {
        entity.writeField("name", name?.trim() ?: "")
        entity.writeField("typ", TextbausteinTyp.fromString(typ))
        entity.writeField("beschreibung", beschreibung)
        entity.writeField("html", html)
        entity.writeField("sortOrder", sortOrder)
        val normalized = LinkedHashSet<String>()
        placeholders.filter { it.isNotBlank() }.map { it.trim() }.forEach { normalized.add(it) }
        entity.writeField("placeholders", normalized)
    }

    companion object {
        @JvmStatic
        fun fromEntity(entity: Textbaustein): TextbausteinDto {
            val typ = entity.readField("typ") as? TextbausteinTyp
            val placeholders = entity.readField("placeholders") as? Set<*> ?: emptySet<Any>()
            val dokumenttypen = entity.readField("dokumenttypen") as? Set<*> ?: emptySet<Any>()
            return TextbausteinDto(
                id = entity.readField("id") as? Long,
                name = entity.readField("name") as? String,
                typ = (typ ?: TextbausteinTyp.FREITEXT).name,
                beschreibung = entity.readField("beschreibung") as? String,
                html = entity.readField("html") as? String,
                placeholders = placeholders.filterIsInstance<String>().toMutableList(),
                dokumenttypen = dokumenttypen.filterIsInstance<Dokumenttyp>().map { it.label }.distinct().toMutableList(),
                sortOrder = entity.readField("sortOrder") as? Int,
                createdAt = entity.readField("createdAt") as? OffsetDateTime,
                updatedAt = entity.readField("updatedAt") as? OffsetDateTime,
            )
        }
    }
}

private fun Textbaustein.readField(name: String): Any? {
    val field: Field = Textbaustein::class.java.getDeclaredField(name)
    field.isAccessible = true
    return field.get(this)
}

private fun Textbaustein.writeField(name: String, value: Any?) {
    val field: Field = Textbaustein::class.java.getDeclaredField(name)
    field.isAccessible = true
    field.set(this, value)
}
