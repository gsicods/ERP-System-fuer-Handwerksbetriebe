package org.example.kalkulationsprogramm.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.util.StringUtils
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

@Service
class ExternalSpamFilterChatBackend @Autowired constructor(
    private val objectMapper: ObjectMapper,
    @Value("\${ai.spamfilter.external.url:}") endpointUrl: String?,
    @Value("\${ai.spamfilter.external.api-key:}") apiKey: String?,
    @Value("\${ai.spamfilter.external.model:}") model: String?,
    @Value("\${ai.spamfilter.external.timeout:30}") private val timeoutSeconds: Int,
) : SpamFilterChatBackend {
    private val httpClient: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()
    private val endpointUrl: String = endpointUrl?.trim() ?: ""
    private val apiKey: String = apiKey?.trim() ?: ""
    private val model: String = model?.trim() ?: ""

    internal constructor(
        objectMapper: ObjectMapper,
        httpClient: HttpClient,
        endpointUrl: String?,
        apiKey: String?,
        model: String?,
        timeoutSeconds: Int,
    ) : this(objectMapper, endpointUrl, apiKey, model, timeoutSeconds) {
        this.injectedHttpClient = httpClient
    }

    private var injectedHttpClient: HttpClient? = null

    override fun identifier(): String = ID

    override fun isEnabled(): Boolean {
        return StringUtils.hasText(endpointUrl) && StringUtils.hasText(model)
    }

    @Throws(IOException::class, InterruptedException::class)
    override fun chat(systemPrompt: String, userMessage: String): String {
        if (!isEnabled()) {
            throw IllegalStateException("Externes Spam-Filter-Backend ist nicht konfiguriert")
        }

        val body = objectMapper.createObjectNode()
        body.put("model", model)
        body.put("temperature", 0.1)

        val messages = objectMapper.createArrayNode()
        messages.add(objectMapper.createObjectNode().put("role", "system").put("content", systemPrompt))
        messages.add(objectMapper.createObjectNode().put("role", "user").put("content", userMessage))
        body.set<com.fasterxml.jackson.databind.JsonNode>("messages", messages)

        val requestBuilder = HttpRequest.newBuilder(URI.create(endpointUrl))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(timeoutSeconds.toLong()))
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))

        if (StringUtils.hasText(apiKey)) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        val response = client().send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        if (response.statusCode() != 200) {
            val statusCode = response.statusCode()
            if (statusCode == 401 || statusCode == 403) {
                log.warn("[SpamFilter/extern] HTTP {} (Auth)", statusCode)
            } else {
                val responseBody = response.body() ?: ""
                val snippet = if (responseBody.length > 200) responseBody.substring(0, 200) + "..." else responseBody
                log.warn("[SpamFilter/extern] HTTP {} - {}", statusCode, snippet)
            }
            throw IOException("Externer Chat-Endpoint Fehler: HTTP $statusCode")
        }

        val choices = objectMapper.readTree(response.body()).path("choices")
        if (choices.isArray && !choices.isEmpty) {
            val content = choices[0].path("message").path("content").asText("")
            if (StringUtils.hasText(content)) {
                return content
            }
        }
        return ""
    }

    private fun client(): HttpClient = injectedHttpClient ?: httpClient

    companion object {
        const val ID: String = "extern"
        private val log = LoggerFactory.getLogger(ExternalSpamFilterChatBackend::class.java)
    }
}
