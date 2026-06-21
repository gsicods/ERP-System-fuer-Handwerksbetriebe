package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.domain.OutOfOfficeSchedule
import org.example.kalkulationsprogramm.repository.EmailSignatureRepository
import org.example.kalkulationsprogramm.repository.OutOfOfficeScheduleRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/email/outofoffice")
class OutOfOfficeController(
    private val repo: OutOfOfficeScheduleRepository,
    private val signatureRepo: EmailSignatureRepository
) {
    @GetMapping
    fun list(): List<OutOfOfficeSchedule> = repo.findAll()

    @GetMapping("/active")
    fun active(): ResponseEntity<OutOfOfficeSchedule> {
        val now = LocalDate.now()
        val activeSchedule = repo
            .findFirstByActiveTrueAndStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByStartAtDesc(now, now)
        return if (activeSchedule.isPresent) {
            ResponseEntity.ok(activeSchedule.get())
        } else {
            ResponseEntity.noContent().build()
        }
    }

    @PostMapping
    fun save(@RequestBody req: SaveOooRequest): ResponseEntity<OutOfOfficeSchedule> {
        val signature = req.signatureId?.let { signatureRepo.findById(it).orElse(null) }
        val schedule = req.id?.let { repo.findById(it).orElse(OutOfOfficeSchedule()) } ?: OutOfOfficeSchedule()
        schedule.title = req.title
        schedule.startAt = req.startDate
        schedule.endAt = req.endDate
        schedule.active = req.active == true
        schedule.subjectTemplate = req.subject
        schedule.bodyTemplate = req.message
        schedule.signature = signature
        return ResponseEntity.ok(repo.save(schedule))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<Void> {
        repo.deleteById(id)
        return ResponseEntity.ok().build()
    }

    class SaveOooRequest {
        var id: Long? = null
        var title: String? = null
        var startDate: LocalDate? = null
        var endDate: LocalDate? = null
        var signatureId: Long? = null
        var active: Boolean? = null
        var subject: String? = null
        var message: String? = null
    }
}
