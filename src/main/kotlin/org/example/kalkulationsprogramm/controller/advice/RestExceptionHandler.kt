package org.example.kalkulationsprogramm.controller.advice

import org.example.kalkulationsprogramm.dto.ApiError
import org.example.kalkulationsprogramm.exception.MietabrechnungValidationException
import org.example.kalkulationsprogramm.util.ConstraintErrorDetail
import org.example.kalkulationsprogramm.util.ConstraintMessageResolver
import org.example.kalkulationsprogramm.util.FieldErrorDetail
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.lang.Nullable
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ResponseStatusException

@ControllerAdvice
class RestExceptionHandler(
    @Nullable private val constraintMessageResolver: ConstraintMessageResolver?,
) {
    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolation(ex: DataIntegrityViolationException): ResponseEntity<ApiError> {
        if (constraintMessageResolver == null) {
            val fallback = ApiError(
                HttpStatus.CONFLICT.value(),
                "Constraint violation",
                null,
                emptyList(),
                ex.mostSpecificCause.message,
            )
            return ResponseEntity.status(HttpStatus.CONFLICT).body(fallback)
        }
        val detail = constraintMessageResolver.resolve(ex)
        LOG.debug("Resolved data integrity violation: {}", detail)
        return ResponseEntity.status(detail.status()).body(toApiError(detail))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val fields = ex.bindingResult.fieldErrors.map { error: FieldError ->
            val field = error.field
            ApiError.Field(field, humanize(field), error.defaultMessage ?: "Ungueltiger Wert.")
        }
        val body = ApiError(
            HttpStatus.BAD_REQUEST.value(),
            "Die Eingaben sind unvollstaendig oder ungueltig.",
            null,
            fields,
            ex.message,
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<ApiError> {
        val body = ApiError(ex.statusCode.value(), ex.reason, null, emptyList(), ex.message)
        return ResponseEntity.status(ex.statusCode).body(body)
    }

    @ExceptionHandler(MietabrechnungValidationException::class)
    fun handleMietabrechnungValidation(ex: MietabrechnungValidationException): ResponseEntity<ApiError> {
        val body = ApiError(ex.status.value(), ex.userMessage, null, emptyList(), ex.detail)
        return ResponseEntity.status(ex.status).body(body)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(RestExceptionHandler::class.java)

        private fun toApiError(detail: ConstraintErrorDetail): ApiError {
            val fields = detail.fieldErrors().map(::toApiField)
            return ApiError(
                detail.status().value(),
                detail.userMessage(),
                detail.constraintName(),
                fields,
                detail.technicalMessage(),
            )
        }

        private fun toApiField(detail: FieldErrorDetail): ApiError.Field {
            return ApiError.Field(detail.field(), detail.label(), detail.message())
        }

        private fun humanize(value: String?): String {
            if (value.isNullOrBlank()) return ""
            val cleaned = value.replace('_', ' ').replace('-', ' ').trim()
            if (cleaned.isEmpty()) return value

            val builder = StringBuilder(cleaned.length)
            var capitalizeNext = true
            for (c in cleaned) {
                if (c.isWhitespace()) {
                    builder.append(' ')
                    capitalizeNext = true
                } else if (capitalizeNext) {
                    builder.append(c.titlecaseChar())
                    capitalizeNext = false
                } else {
                    builder.append(c.lowercaseChar())
                }
            }
            return builder.toString()
        }
    }
}
