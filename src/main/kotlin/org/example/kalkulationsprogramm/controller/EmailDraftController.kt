package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.domain.EmailDraft
import org.example.kalkulationsprogramm.repository.EmailDraftRepository
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/emails/drafts")
class EmailDraftController(
    private val draftRepository: EmailDraftRepository
) {
    @GetMapping
    fun getAllDrafts(): List<EmailDraft> = draftRepository.findAllByOrderByUpdatedAtDesc()

    @GetMapping("/count")
    fun getDraftCount(): Map<String, Long> = mapOf("count" to draftRepository.count())

    @PostMapping
    @Transactional
    fun createDraft(@RequestBody draft: EmailDraft): ResponseEntity<EmailDraft> {
        draft.id = null
        return ResponseEntity.ok(draftRepository.save(draft))
    }

    @PutMapping("/{id}")
    @Transactional
    fun updateDraft(@PathVariable id: Long, @RequestBody draft: EmailDraft): ResponseEntity<EmailDraft> {
        val existing = draftRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
        existing.recipient = draft.recipient
        existing.cc = draft.cc
        existing.subject = draft.subject
        existing.body = draft.body
        existing.fromAddress = draft.fromAddress
        existing.replyEmailId = draft.replyEmailId
        existing.projektId = draft.projektId
        existing.anfrageId = draft.anfrageId
        return ResponseEntity.ok(draftRepository.save(existing))
    }

    @DeleteMapping("/{id}")
    @Transactional
    fun deleteDraft(@PathVariable id: Long): ResponseEntity<Void> {
        if (!draftRepository.existsById(id)) {
            return ResponseEntity.notFound().build()
        }
        draftRepository.deleteById(id)
        return ResponseEntity.ok().build()
    }
}
