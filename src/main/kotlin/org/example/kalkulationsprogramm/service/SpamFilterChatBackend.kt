package org.example.kalkulationsprogramm.service

interface SpamFilterChatBackend {
    fun identifier(): String

    fun isEnabled(): Boolean

    @Throws(Exception::class)
    fun chat(systemPrompt: String, userMessage: String): String
}
