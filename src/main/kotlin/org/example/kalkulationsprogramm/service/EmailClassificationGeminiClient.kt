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
class EmailClassificationGeminiClient @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val systemSettingsService: SystemSettingsService,
    @Value("\${ai.gemini.model.email-classification:gemini-2.5-flash-lite}")
    private val model: String
) {
    private val httpClient: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()

    internal constructor(
        objectMapper: ObjectMapper,
        systemSettingsService: SystemSettingsService,
        httpClient: HttpClient,
        model: String
    ) : this(objectMapper, systemSettingsService, model) {
        this.injectedHttpClient = httpClient
    }

    private var injectedHttpClient: HttpClient? = null

    fun isEnabled(): Boolean {
        val key = systemSettingsService.geminiApiKey
        return !key.isNullOrBlank() && key != "OVERRIDE_IN_LOCAL"
    }

    @Throws(IOException::class, InterruptedException::class)
    fun chat(systemPrompt: String, userMessage: String): String {
        val apiKey = systemSettingsService.geminiApiKey
        if (apiKey.isNullOrBlank() || apiKey == "OVERRIDE_IN_LOCAL") {
            throw IOException("Gemini API Key fehlt (ai.gemini.api-key)")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s".format(model, apiKey)
        val requestBody = objectMapper.createObjectNode()

        val systemInstruction = objectMapper.createObjectNode()
        val sysParts = objectMapper.createArrayNode()
        sysParts.add(objectMapper.createObjectNode().put("text", systemPrompt))
        systemInstruction.set<com.fasterxml.jackson.databind.JsonNode>("parts", sysParts)
        requestBody.set<com.fasterxml.jackson.databind.JsonNode>("systemInstruction", systemInstruction)

        val contents = objectMapper.createArrayNode()
        val userMsg = objectMapper.createObjectNode().put("role", "user")
        val parts = objectMapper.createArrayNode()
        parts.add(objectMapper.createObjectNode().put("text", userMessage))
        userMsg.set<com.fasterxml.jackson.databind.JsonNode>("parts", parts)
        contents.add(userMsg)
        requestBody.set<com.fasterxml.jackson.databind.JsonNode>("contents", contents)

        val config = objectMapper.createObjectNode()
        config.put("temperature", 0.1)
        config.put("responseMimeType", "application/json")
        requestBody.set<com.fasterxml.jackson.databind.JsonNode>("generationConfig", config)

        val request = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody), StandardCharsets.UTF_8))
            .build()

        val response = (injectedHttpClient ?: httpClient).send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        if (response.statusCode() != 200) {
            log.warn("[Gemini-Classify] HTTP {} - {}", response.statusCode(), response.body())
            throw IOException("Gemini API Error: ${response.statusCode()}")
        }

        val root = objectMapper.readTree(response.body())
        val candidates = root.path("candidates")
        if (candidates.isArray && !candidates.isEmpty) {
            val partsNode = candidates[0].path("content").path("parts")
            if (partsNode.isArray && !partsNode.isEmpty) {
                return partsNode[0].path("text").asText()
            }
        }
        return ""
    }

    companion object {
        private val log = LoggerFactory.getLogger(EmailClassificationGeminiClient::class.java)
    }
}
