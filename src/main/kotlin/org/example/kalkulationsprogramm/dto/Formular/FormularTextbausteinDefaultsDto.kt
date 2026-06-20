package org.example.kalkulationsprogramm.dto.Formular

class FormularTextbausteinDefaultsDto {
    var entries: MutableList<Entry> = ArrayList()

    data class Entry(
        var dokumenttyp: String? = null,
        var vortextIds: MutableList<Long> = ArrayList(),
        var nachtextIds: MutableList<Long> = ArrayList(),
    )
}
