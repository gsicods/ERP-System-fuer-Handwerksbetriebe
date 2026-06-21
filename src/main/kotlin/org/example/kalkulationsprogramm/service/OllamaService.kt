package org.example.kalkulationsprogramm.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

@Service
class OllamaService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    @Value("\${ai.ollama.url:http://localhost:11434}") private val ollamaBaseUrl: String,
    @Value("\${ai.ollama.model:qwen3:8b}") private val defaultModel: String,
    @Value("\${ai.ollama.timeout:120}") private val timeoutSeconds: Int,
    @Value("\${ai.ollama.enabled:true}") val isEnabled: Boolean,
) {
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    internal constructor(
        objectMapper: ObjectMapper,
        httpClient: HttpClient,
        ollamaBaseUrl: String,
        defaultModel: String,
        timeoutSeconds: Int,
        enabled: Boolean,
    ) : this(objectMapper, ollamaBaseUrl, defaultModel, timeoutSeconds, enabled) {
        this.injectedHttpClient = httpClient
    }

    private var injectedHttpClient: HttpClient? = null

    @Throws(IOException::class, InterruptedException::class)
    fun chat(systemPrompt: String, userMessage: String): String {
        return chat(systemPrompt, userMessage, defaultModel)
    }

    @Throws(IOException::class, InterruptedException::class)
    fun chat(systemPrompt: String, userMessage: String, model: String): String {
        if (!isEnabled) {
            throw IllegalStateException("Ollama ist deaktiviert (ai.ollama.enabled=false)")
        }

        val url = "$ollamaBaseUrl/api/chat"
        val requestBody = objectMapper.createObjectNode()
        requestBody.put("model", model)
        requestBody.put("stream", false)

        val options = objectMapper.createObjectNode()
        options.put("temperature", 0.1)
        options.put("num_predict", 500)
        requestBody.set<com.fasterxml.jackson.databind.JsonNode>("options", options)

        val messages = objectMapper.createArrayNode()
        messages.add(objectMapper.createObjectNode().put("role", "system").put("content", systemPrompt))
        messages.add(objectMapper.createObjectNode().put("role", "user").put("content", userMessage))
        requestBody.set<com.fasterxml.jackson.databind.JsonNode>("messages", messages)

        val body = objectMapper.writeValueAsString(requestBody)
        log.debug("[Ollama] Request an {}: model={}, prompt-length={}", url, model, userMessage.length)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
            .timeout(Duration.ofSeconds(timeoutSeconds.toLong()))
            .build()

        val response = client().send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            log.error("[Ollama] Fehler: HTTP {} - {}", response.statusCode(), response.body())
            throw IOException("Ollama API Fehler: HTTP ${response.statusCode()}")
        }

        val messageNode = objectMapper.readTree(response.body()).path("message").path("content")
        if (messageNode.isMissingNode || messageNode.asText().isBlank()) {
            throw IOException("Ollama: Leere Antwort erhalten")
        }

        val result = messageNode.asText().trim()
        log.debug("[Ollama] Antwort erhalten: {} Zeichen", result.length)
        return result
    }

    fun isAvailable(): Boolean {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create("$ollamaBaseUrl/api/tags"))
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build()
            client().send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200
        } catch (e: Exception) {
            log.debug("[Ollama] Server nicht erreichbar: {}", e.message)
            false
        }
    }

    private fun client(): HttpClient = injectedHttpClient ?: httpClient

    companion object {
        private val log = LoggerFactory.getLogger(OllamaService::class.java)
    }
}
