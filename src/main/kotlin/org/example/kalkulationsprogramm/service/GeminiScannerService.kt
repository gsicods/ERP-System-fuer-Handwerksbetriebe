package org.example.kalkulationsprogramm.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.Base64

@Service
class GeminiScannerService(
    private val objectMapper: ObjectMapper,
    private val systemSettingsService: SystemSettingsService,
) {
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build()

    @Value("\${ai.gemini.model.scanner:gemini-3-flash-preview}")
    private lateinit var geminiModel: String

    fun generateFilename(imageBytes: ByteArray): String {
        return try {
            val geminiApiKey = systemSettingsService.geminiApiKey
            if (geminiApiKey.isNullOrBlank()) {
                return "Scan_${System.currentTimeMillis()}"
            }

            val base64Image = Base64.getEncoder().encodeToString(imageBytes)
            val requestBody = objectMapper.createObjectNode()

            val systemInstruction = objectMapper.createObjectNode()
            val sysParts = objectMapper.createArrayNode()
            sysParts.add(objectMapper.createObjectNode().put("text", SYSTEM_PROMPT_FILENAME))
            systemInstruction.set<com.fasterxml.jackson.databind.JsonNode>("parts", sysParts)
            requestBody.set<com.fasterxml.jackson.databind.JsonNode>("systemInstruction", systemInstruction)

            val contents = objectMapper.createArrayNode()
            val userMsg = objectMapper.createObjectNode().put("role", "user")
            val parts = objectMapper.createArrayNode()

            val imgPart = objectMapper.createObjectNode()
            val inlineData = objectMapper.createObjectNode()
            inlineData.put("mimeType", "image/jpeg")
            inlineData.put("data", base64Image)
            imgPart.set<com.fasterxml.jackson.databind.JsonNode>("inlineData", inlineData)
            parts.add(imgPart)
            parts.add(objectMapper.createObjectNode().put("text", "Generate a filename."))

            userMsg.set<com.fasterxml.jackson.databind.JsonNode>("parts", parts)
            contents.add(userMsg)
            requestBody.set<com.fasterxml.jackson.databind.JsonNode>("contents", contents)

            val config = objectMapper.createObjectNode()
            config.put("temperature", 0.0)
            config.put("responseMimeType", "text/plain")
            requestBody.set<com.fasterxml.jackson.databind.JsonNode>("generationConfig", config)

            val body = objectMapper.writeValueAsString(requestBody)
            val apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/$geminiModel:generateContent?key=$geminiApiKey"
            val request = HttpRequest.newBuilder(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            if (response.statusCode() != 200) {
                log.error("Gemini Naming Error {}: {}", response.statusCode(), response.body())
                return "Scan_${System.currentTimeMillis()}"
            }

            val root = objectMapper.readTree(response.body())
            val text = root.path("candidates")[0].path("content").path("parts")[0].path("text").asText()
                .trim()
                .replace(" ", "_")
                .replace("[^a-zA-Z0-9_\\-]".toRegex(), "")
            if (text.isEmpty()) "Scan_${System.currentTimeMillis()}" else text
        } catch (e: Exception) {
            log.error("Failed to generate filename", e)
            "Scan_${System.currentTimeMillis()}"
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(GeminiScannerService::class.java)

        private val SYSTEM_PROMPT_FILENAME = """
            You are a smart file naming assistant.
            Analyze the document image and generate a concise, professional filename.

            Format: "{Type}_{Sender/Context}_{Date}"
            - Type: Rechnung, Vertrag, Brief, Notiz, etc. (German)
            - Sender: Company name or person if visible.
            - Date: YYYY-MM-DD (from the document) or TODAY if not found.

            Examples:
            - Rechnung_Amazon_2023-12-24
            - Brief_Finanzamt_2024-01-05
            - Notiz_Meeting_2024-03-10

            Rules:
            1. Return ONLY the filename string.
            2. No file extension.
            3. No spaces (use underscores).
            4. If text is illegible, return "Scan_{Timestamp}".
        """.trimIndent()
    }
}
