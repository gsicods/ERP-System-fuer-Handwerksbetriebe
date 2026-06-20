package org.example.kalkulationsprogramm.dto.Formular

import org.example.kalkulationsprogramm.domain.Textbaustein
import java.lang.reflect.Field

class FormularTextbausteinResolvedDto {
    var vortexte: MutableList<Item> = ArrayList()
    var nachtexte: MutableList<Item> = ArrayList()

    data class Item(
        var id: Long? = null,
        var name: String? = null,
        var html: String? = null,
        var beschreibung: String? = null,
    ) {
        companion object {
            @JvmStatic
            fun from(tb: Textbaustein): Item = Item(
                tb.readField("id") as? Long,
                tb.readField("name") as? String,
                tb.readField("html") as? String,
                tb.readField("beschreibung") as? String,
            )
        }
    }

    companion object {
        @JvmStatic
        fun from(vor: List<Textbaustein>?, nach: List<Textbaustein>?): FormularTextbausteinResolvedDto {
            val dto = FormularTextbausteinResolvedDto()
            vor?.forEach { dto.vortexte.add(Item.from(it)) }
            nach?.forEach { dto.nachtexte.add(Item.from(it)) }
            return dto
        }
    }
}

private fun Textbaustein.readField(name: String): Any? {
    val field: Field = Textbaustein::class.java.getDeclaredField(name)
    field.isAccessible = true
    return field.get(this)
}
