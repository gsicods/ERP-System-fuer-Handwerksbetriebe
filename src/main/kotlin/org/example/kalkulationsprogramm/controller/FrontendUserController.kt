package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.domain.FrontendUserProfile
import org.example.kalkulationsprogramm.domain.FrontendUserRole
import org.example.kalkulationsprogramm.service.FrontendUserProfileService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.LinkedHashSet
import java.util.Locale
import java.util.NoSuchElementException

@RestController
@RequestMapping("/api/frontend-users")
class FrontendUserController(
    private val profileService: FrontendUserProfileService,
) {
    @GetMapping
    fun list(): List<FrontendUserProfile> = profileService.list()

    @PostMapping
    fun save(@RequestBody request: SaveProfileRequest): ResponseEntity<*> {
        val displayName = request.displayName
        if (displayName.isNullOrBlank()) {
            return ResponseEntity.badRequest().body(mapOf("message" to "Anzeigename darf nicht leer sein."))
        }

        val profile = FrontendUserProfile()
        profile.id = request.id
        profile.displayName = displayName.trim()
        profile.shortCode = request.shortCode?.trim()?.takeIf { it.isNotBlank() }

        return try {
            val saved = profileService.saveOrUpdate(
                profile,
                request.defaultSignatureId,
                request.mitarbeiterId,
                request.username,
                request.password,
                request.rolesAsEnum,
                request.active,
                request.emailAbsenderId,
            )
            ResponseEntity.ok(saved)
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("message" to ex.message))
        } catch (ex: IllegalStateException) {
            ResponseEntity.badRequest().body(mapOf("message" to ex.message))
        } catch (ex: NoSuchElementException) {
            ResponseEntity.notFound().build<Any>()
        }
    }

    @PostMapping("/{id}/default-signature")
    fun setDefaultSignature(
        @PathVariable id: Long,
        @RequestBody request: SetDefaultSignatureRequest,
    ): ResponseEntity<FrontendUserProfile> =
        try {
            ResponseEntity.ok(profileService.setDefaultSignature(id, request.signatureId))
        } catch (ex: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<*> =
        try {
            profileService.delete(id)
            ResponseEntity.noContent().build<Any>()
        } catch (ex: IllegalStateException) {
            ResponseEntity.badRequest().body(mapOf("message" to ex.message))
        } catch (ex: Exception) {
            ResponseEntity.notFound().build<Any>()
        }

    data class SaveProfileRequest(
        var id: Long? = null,
        var displayName: String? = null,
        var shortCode: String? = null,
        var username: String? = null,
        var password: String? = null,
        var roles: List<String>? = null,
        var active: Boolean? = null,
        var defaultSignatureId: Long? = null,
        var mitarbeiterId: Long? = null,
        var emailAbsenderId: Long? = null,
    ) {
        val rolesAsEnum: Set<FrontendUserRole>
            get() {
                val result = LinkedHashSet<FrontendUserRole>()
                roles?.forEach { role ->
                    if (!role.isNullOrBlank()) {
                        try {
                            result.add(FrontendUserRole.valueOf(role.trim().uppercase(Locale.ROOT)))
                        } catch (_: IllegalArgumentException) {
                        }
                    }
                }
                return result
            }
    }

    data class SetDefaultSignatureRequest(
        var signatureId: Long? = null,
    )
}
