package org.example.kalkulationsprogramm.controller

import org.example.kalkulationsprogramm.service.VendorInvoiceIntegrationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/vendor-invoices")
class VendorInvoiceController(
    private val integrationService: VendorInvoiceIntegrationService
) {
    @GetMapping("/status")
    fun getStatus(): ResponseEntity<Map<String, Any>> =
        ResponseEntity.ok(integrationService.integrationStatus)

    @PostMapping("/fetch")
    fun fetchInvoices(): ResponseEntity<Map<String, Any>> =
        ResponseEntity.ok(integrationService.fetchAllVendorInvoices())

    @PostMapping("/fetch/microsoft")
    fun fetchMicrosoftInvoices(): ResponseEntity<Map<String, Any?>> =
        try {
            val count = integrationService.fetchMicrosoftInvoices()
            ResponseEntity.ok(mapOf("success" to true, "count" to count))
        } catch (e: Exception) {
            ResponseEntity.ok(mapOf("success" to false, "error" to e.message))
        }

    @PostMapping("/fetch/amazon")
    fun fetchAmazonInvoices(): ResponseEntity<Map<String, Any?>> =
        try {
            val count = integrationService.fetchAmazonInvoices()
            ResponseEntity.ok(mapOf("success" to true, "count" to count))
        } catch (e: Exception) {
            ResponseEntity.ok(mapOf("success" to false, "error" to e.message))
        }
}
