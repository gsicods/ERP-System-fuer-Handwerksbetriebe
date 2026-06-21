package org.example.kalkulationsprogramm.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
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
class QdrantRagService(
    private val objectMapper: ObjectMapper,
    private val systemSettingsService: SystemSettingsService,
) {
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    @Value("\${ai.rag.qdrant.host:localhost}")
    private lateinit var qdrantHost: String

    @Value("\${ai.rag.qdrant.port:6333}")
    private var qdrantPort: Int = 6333

    @Value("\${ai.rag.qdrant.collection:codebase}")
    private lateinit var qdrantCollection: String

    @Value("\${ai.rag.top-k:10}")
    private var topK: Int = 10

    @Value("\${ai.rag.score-threshold:0.3}")
    private var scoreThreshold: Double = 0.3

    @Value("\${ai.rag.enabled:false}")
    private var ragEnabled: Boolean = false

    data class CodeChunkResult(
        val content: String,
        val filePath: String,
        val category: String,
        val chunkType: String,
        val name: String,
        val score: Double,
    )

    fun isEnabled(): Boolean = ragEnabled

    fun isAvailable(): Boolean {
        if (!ragEnabled) return false
        return try {
            val request = HttpRequest.newBuilder(
                URI.create("http://$qdrantHost:$qdrantPort/collections/$qdrantCollection"),
            )
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build()
            httpClient.send(request, HttpResponse.BodyHandlers.ofString()).statusCode() == 200
        } catch (e: Exception) {
            log.debug("Qdrant nicht erreichbar: {}", e.message)
            false
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    fun search(query: String, pageContext: String?): List<CodeChunkResult> {
        val enrichedQuery = if (!pageContext.isNullOrBlank()) {
            "$query\n\nAktueller Seitenkontext: $pageContext"
        } else {
            query
        }

        val embedStart = System.currentTimeMillis()
        val queryVector = embedQuery(enrichedQuery)
        log.info("    Embedding erstellt in {} ms", System.currentTimeMillis() - embedStart)

        val searchStart = System.currentTimeMillis()
        val results = searchQdrant(queryVector)
        log.info(
            "    Qdrant-Suche: {} Treffer in {} ms (top-k={}, threshold={})",
            results.size,
            System.currentTimeMillis() - searchStart,
            topK,
            scoreThreshold,
        )
        return results
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun embedQuery(query: String): List<Double> {
        val url = GEMINI_EMBED_URL.format(systemSettingsService.geminiApiKey)
        val requestBody = objectMapper.createObjectNode()
        val content = objectMapper.createObjectNode()
        val parts = objectMapper.createArrayNode()
        parts.add(objectMapper.createObjectNode().put("text", query))
        content.set<com.fasterxml.jackson.databind.JsonNode>("parts", parts)
        requestBody.set<com.fasterxml.jackson.databind.JsonNode>("content", content)
        requestBody.put("taskType", "RETRIEVAL_QUERY")

        val request = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(15))
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody), StandardCharsets.UTF_8))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        if (response.statusCode() != 200) {
            log.error("Gemini Embedding API Error {}: {}", response.statusCode(), response.body())
            throw IOException("Embedding-Fehler (Status ${response.statusCode()})")
        }

        val values = objectMapper.readTree(response.body()).path("embedding").path("values")
        if (!values.isArray || values.isEmpty) {
            throw IOException("Keine Embedding-Werte erhalten")
        }

        val vector = ArrayList<Double>(EMBEDDING_DIMENSION)
        for (value in values) {
            vector.add(value.asDouble())
        }
        return vector
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun searchQdrant(queryVector: List<Double>): List<CodeChunkResult> {
        val url = "http://$qdrantHost:$qdrantPort/collections/$qdrantCollection/points/search"
        val requestBody = objectMapper.createObjectNode()
        val vectorNode = objectMapper.createArrayNode()
        queryVector.forEach { vectorNode.add(it) }
        requestBody.set<com.fasterxml.jackson.databind.JsonNode>("vector", vectorNode)
        requestBody.put("limit", topK)
        requestBody.put("score_threshold", scoreThreshold)
        requestBody.put("with_payload", true)

        val request = HttpRequest.newBuilder(URI.create(url))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody), StandardCharsets.UTF_8))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        if (response.statusCode() != 200) {
            log.error("Qdrant search error {}: {}", response.statusCode(), response.body())
            throw IOException("Qdrant-Suche fehlgeschlagen (Status ${response.statusCode()})")
        }

        val results = objectMapper.readTree(response.body()).path("result")
        val chunks = mutableListOf<CodeChunkResult>()
        if (results.isArray) {
            for (hit in results) {
                val payload = hit.path("payload")
                chunks.add(
                    CodeChunkResult(
                        payload.path("content").asText(),
                        payload.path("file_path").asText(),
                        payload.path("category").asText(),
                        payload.path("chunk_type").asText(),
                        payload.path("name").asText(),
                        hit.path("score").asDouble(),
                    ),
                )
            }
        }

        log.debug("Qdrant-Suche: {} Treffer (Schwelle: {})", chunks.size, scoreThreshold)
        return chunks
    }

    fun buildContextFromResults(results: List<CodeChunkResult>): String {
        if (results.isEmpty()) return ""

        val sb = StringBuilder()
        sb.append("## Relevante Code-Abschnitte (automatisch per Vektor-Suche gefunden)\n\n")
        results.forEachIndexed { index, result ->
            sb.append(
                "### [%d] %s — %s (%s) · Score: %.2f\n".format(
                    index + 1,
                    result.filePath,
                    result.name,
                    result.chunkType,
                    result.score,
                ),
            )
            sb.append("```\n")
            sb.append(result.content)
            sb.append("\n```\n\n")
        }
        return sb.toString()
    }

    companion object {
        private val log = LoggerFactory.getLogger(QdrantRagService::class.java)
        private const val GEMINI_EMBED_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=%s"
        private const val EMBEDDING_DIMENSION = 768
    }
}
