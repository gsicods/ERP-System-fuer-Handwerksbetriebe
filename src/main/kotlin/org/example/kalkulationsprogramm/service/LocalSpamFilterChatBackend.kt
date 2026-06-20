package org.example.kalkulationsprogramm.service

import org.springframework.stereotype.Service

@Service
class LocalSpamFilterChatBackend(private val ollamaService: OllamaService) : SpamFilterChatBackend {
    override fun identifier(): String = ID
    override fun isEnabled(): Boolean = ollamaService.isEnabled
    override fun chat(systemPrompt: String, userMessage: String): String = ollamaService.chat(systemPrompt, userMessage)

    companion object {
        const val ID: String = "lokal"
    }
}
