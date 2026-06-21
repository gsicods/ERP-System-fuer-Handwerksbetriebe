package org.example.kalkulationsprogramm.controller

import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.servlet.error.ErrorAttributes
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.context.request.ServletWebRequest

@Controller
@RequestMapping("/error")
class SpaErrorController(
    private val errorAttributes: ErrorAttributes
) : ErrorController {
    @RequestMapping(produces = [MediaType.TEXT_HTML_VALUE])
    fun handleHtml(request: HttpServletRequest): Any {
        val status = resolveStatus(request)
        val uri = resolveOriginalUri(request)
        if (status == HttpStatus.NOT_FOUND.value() && isSpaNavigationCandidate(uri)) {
            return "forward:/index.html"
        }
        return ResponseEntity.status(HttpStatus.valueOf(status))
            .contentType(MediaType.TEXT_HTML)
            .body(buildMinimalHtml(status, uri))
    }

    @RequestMapping
    fun handleJson(request: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        val status = resolveStatus(request)
        val body = HashMap(errorAttributes.getErrorAttributes(ServletWebRequest(request), ErrorAttributeOptions.defaults()))
        return ResponseEntity.status(HttpStatus.valueOf(status)).body(body)
    }

    private fun resolveStatus(request: HttpServletRequest): Int {
        val statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) ?: return HttpStatus.INTERNAL_SERVER_ERROR.value()
        return statusObj.toString().toIntOrNull() ?: HttpStatus.INTERNAL_SERVER_ERROR.value()
    }

    private fun resolveOriginalUri(request: HttpServletRequest): String =
        request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI)?.toString() ?: ""

    private fun isSpaNavigationCandidate(uri: String?): Boolean {
        if (uri.isNullOrEmpty()) return false
        if (uri.startsWith("/api/") || uri == "/api") return false
        if (uri.startsWith("/zeiterfassung/") || uri == "/zeiterfassung") return false
        if (uri.startsWith("/actuator/") || uri == "/actuator") return false
        if (uri.startsWith("/error")) return false
        val last = uri.substringAfterLast('/')
        return !last.contains(".")
    }

    private fun buildMinimalHtml(status: Int, uri: String?): String {
        val safePath = uri?.replace("<", "&lt;")?.replace(">", "&gt;") ?: ""
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Fehler $status</title></head>" +
            "<body style=\"font-family:sans-serif;padding:2rem\"><h1>Fehler $status</h1><p>Pfad: $safePath</p></body></html>"
    }
}
