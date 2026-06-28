package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.domain.Mitarbeiter
import org.example.kalkulationsprogramm.repository.MitarbeiterRepository
import org.example.kalkulationsprogramm.service.WebPushService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/push")
class PushSubscriptionController(
    private val webPushService: WebPushService,
    private val mitarbeiterRepository: MitarbeiterRepository,
) {
    @GetMapping("/vapid-key")
    fun getVapidPublicKey(@RequestParam token: String): ResponseEntity<Map<String, String>> {
        val mitarbeiter = mitarbeiterRepository.findByLoginToken(token).orElse(null)
        if (mitarbeiter == null || readBooleanProperty(mitarbeiter, "aktiv") != true) {
            return ResponseEntity.status(401).build()
        }

        if (!webPushService.isEnabled) {
            return ResponseEntity.ok(mapOf("publicKey" to "", "enabled" to "false"))
        }

        return ResponseEntity.ok(
            mapOf(
                "publicKey" to webPushService.vapidPublicKey,
                "enabled" to "true",
            )
        )
    }

    @PostMapping("/subscribe")
    fun subscribe(
        @RequestParam token: String,
        @RequestBody request: PushSubscribeRequest,
    ): ResponseEntity<Map<String, Boolean>> {
        val mitarbeiter = mitarbeiterRepository.findByLoginToken(token).orElse(null)
        if (mitarbeiter == null || readBooleanProperty(mitarbeiter, "aktiv") != true) {
            return ResponseEntity.status(401).build()
        }

        if (!webPushService.isEnabled) {
            return ResponseEntity.ok(mapOf("subscribed" to false))
        }

        webPushService.subscribe(
            readLongProperty(mitarbeiter, "id"),
            request.endpoint,
            request.p256dh,
            request.auth,
        )

        return ResponseEntity.ok(mapOf("subscribed" to true))
    }

    @PostMapping("/unsubscribe")
    fun unsubscribe(
        @RequestParam token: String,
        @RequestBody body: Map<String, String>,
    ): ResponseEntity<Map<String, Boolean>> {
        val mitarbeiter = mitarbeiterRepository.findByLoginToken(token).orElse(null)
        if (mitarbeiter == null || readBooleanProperty(mitarbeiter, "aktiv") != true) {
            return ResponseEntity.status(401).build()
        }

        val endpoint = body["endpoint"]
        if (!endpoint.isNullOrBlank()) {
            webPushService.unsubscribe(endpoint)
        }

        return ResponseEntity.ok(mapOf("unsubscribed" to true))
    }

    data class PushSubscribeRequest(
        val endpoint: String?,
        val p256dh: String?,
        val auth: String?,
    )

    companion object {
        private fun readBooleanProperty(mitarbeiter: Mitarbeiter, property: String): Boolean? =
            readProperty(mitarbeiter, property) as? Boolean

        private fun readLongProperty(mitarbeiter: Mitarbeiter, property: String): Long? =
            readProperty(mitarbeiter, property) as? Long

        private fun readProperty(target: Any, property: String): Any? {
            val suffix = property.replaceFirstChar { it.uppercaseChar() }
            return target.javaClass.methods
                .firstOrNull { it.parameterCount == 0 && (it.name == "get$suffix" || it.name == "is$suffix") }
                ?.invoke(target)
        }
    }
}
