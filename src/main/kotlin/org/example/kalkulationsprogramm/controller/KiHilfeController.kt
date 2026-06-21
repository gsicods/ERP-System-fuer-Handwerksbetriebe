package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.service.KiHilfeService
import org.example.kalkulationsprogramm.service.KiHilfeService.ChatMessage
import org.example.kalkulationsprogramm.service.KiHilfeService.PageContext
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/ki-hilfe")
class KiHilfeController(
    private val kiHilfeService: KiHilfeService
) {
    data class ChatRequest(val messages: List<MessageDto>?, val context: PageContextDto?)
    data class MessageDto(val role: String?, val text: String?)
    data class PageContextDto(
        val route: String?,
        val pageTitle: String?,
        val visibleContent: String?,
        val errorMessages: String?,
        val latitude: Double?,
        val longitude: Double?
    )

    @PostMapping("/chat")
    fun chat(@RequestBody request: ChatRequest): ResponseEntity<*> {
        val requestMessages = request.messages
        if (requestMessages.isNullOrEmpty()) return ResponseEntity.badRequest().body(mapOf("error" to "Keine Nachricht angegeben"))
        if (requestMessages.size > 50) return ResponseEntity.badRequest().body(mapOf("error" to "Zu viele Nachrichten"))
        for (msg in requestMessages) {
            if (msg.text.isNullOrBlank()) return ResponseEntity.badRequest().body(mapOf("error" to "Leere Nachricht"))
            if (msg.text.length > 5000) return ResponseEntity.badRequest().body(mapOf("error" to "Nachricht zu lang (max. 5000 Zeichen)"))
            if (msg.role != "user" && msg.role != "assistant") {
                return ResponseEntity.badRequest().body(mapOf("error" to "Ungueltige Rolle: ${msg.role}"))
            }
        }

        return try {
            val messages = requestMessages.map { ChatMessage(it.role, it.text) }
            val pageContext = request.context?.let {
                log.info(
                    "\n=====================================================" +
                        "\n| KI-HILFE ANFRAGE" +
                        "\n| Route:   {}" +
                        "\n| Titel:   {}" +
                        "\n| Fehler:  {}" +
                        "\n| Kontext: {} Zeichen" +
                        "\n=====================================================",
                    it.route,
                    it.pageTitle,
                    it.errorMessages ?: "(keine)",
                    it.visibleContent?.length ?: 0
                )
                PageContext(it.route, it.pageTitle, it.visibleContent, it.errorMessages, it.latitude, it.longitude)
            } ?: run {
                log.info("KI-Hilfe Anfrage OHNE Seitenkontext")
                null
            }
            val lastUserMsg = messages.filter { it.role() == "user" }.lastOrNull()?.text().orEmpty()
            log.info("| Frage:   {}", if (lastUserMsg.length > 100) lastUserMsg.substring(0, 100) + "..." else lastUserMsg)
            val result = kiHilfeService.chat(messages, pageContext)
            log.info("| Antwort: {} Zeichen", result.reply().length)
            val response = HashMap<String, Any>()
            response["reply"] = result.reply()
            if (!result.sources().isNullOrEmpty()) response["sources"] = result.sources()
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            log.error("KI-Hilfe Fehler", e)
            ResponseEntity.internalServerError().body(mapOf("error" to "KI-Hilfe ist gerade nicht verfuegbar. Bitte versuche es spaeter erneut."))
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KiHilfeController::class.java)
    }
}
