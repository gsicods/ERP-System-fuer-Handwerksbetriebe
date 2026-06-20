package org.example.kalkulationsprogramm.dto

data class ContactDto(
    var id: String? = null,
    var name: String? = null,
    var email: String? = null,
    var type: String? = null,
    var context: String? = null,
) {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var id: String? = null
        private var name: String? = null
        private var email: String? = null
        private var type: String? = null
        private var context: String? = null

        fun id(id: String?) = apply { this.id = id }
        fun name(name: String?) = apply { this.name = name }
        fun email(email: String?) = apply { this.email = email }
        fun type(type: String?) = apply { this.type = type }
        fun context(context: String?) = apply { this.context = context }
        fun build(): ContactDto = ContactDto(id, name, email, type, context)
    }
}
