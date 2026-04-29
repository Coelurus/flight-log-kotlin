package eu.profinit.flightlog.web

import eu.profinit.flightlog.operation.BusinessRuleException
import eu.profinit.flightlog.security.AccountLockedLoginException
import eu.profinit.flightlog.security.BadCredentialsLoginException
import eu.profinit.flightlog.security.CaptchaRequiredException
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    data class ApiError(
        val code: String,
        val message: String,
        val fieldErrors: Map<String, String>? = null,
    )

    @ExceptionHandler(BadCredentialsLoginException::class)
    fun badCreds(ex: BadCredentialsLoginException) =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError("bad_credentials", ex.message ?: "Neplatný e-mail nebo heslo."))

    @ExceptionHandler(CaptchaRequiredException::class)
    fun captcha(ex: CaptchaRequiredException) =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError("captcha_required", "Vyžadováno ověření CAPTCHA."))

    @ExceptionHandler(AccountLockedLoginException::class)
    fun locked(ex: AccountLockedLoginException) =
        ResponseEntity.status(HttpStatus.LOCKED).body(ApiError("account_locked", "Účet je dočasně zablokován."))

    @ExceptionHandler(BusinessRuleException::class)
    fun business(ex: BusinessRuleException): ResponseEntity<ApiError> {
        logger.warn("Business rule failed: {}", ex.message)
        val fields = ex.field?.let { mapOf(it to (ex.message ?: "")) }
        return ResponseEntity.badRequest().body(ApiError("business_rule", ex.message ?: "Pravidlo nesplněno.", fields))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun validation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val fields = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "neplatné") }
        return ResponseEntity.badRequest().body(ApiError("validation_failed", "Neplatná data.", fields))
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun constraint(ex: ConstraintViolationException): ResponseEntity<ApiError> {
        val fields = ex.constraintViolations.associate { it.propertyPath.toString() to it.message }
        return ResponseEntity.badRequest().body(ApiError("validation_failed", "Neplatná data.", fields))
    }

    @ExceptionHandler(EntityNotFoundException::class, NoSuchElementException::class)
    fun notFound(ex: Exception) =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError("not_found", ex.message ?: "Nenalezeno."))

    @ExceptionHandler(IllegalArgumentException::class)
    fun bad(ex: IllegalArgumentException) =
        ResponseEntity.badRequest().body(ApiError("bad_request", ex.message ?: "Neplatný požadavek."))

    @ExceptionHandler(AccessDeniedException::class)
    fun forbidden(ex: AccessDeniedException) =
        ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError("forbidden", "Přístup zamítnut."))

    @ExceptionHandler(NoResourceFoundException::class)
    fun staticNotFound(ex: NoResourceFoundException): ResponseEntity<ApiError> {
        // Quietly return 404 for missing static assets; no stack trace spam.
        logger.debug("Static resource missing: {}", ex.resourcePath)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError("not_found", "Soubor nenalezen."))
    }

    @ExceptionHandler(Exception::class)
    fun unknown(ex: Exception): ResponseEntity<ApiError> {
        logger.error("Unhandled error: {}", ex.message, ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError("internal_error", "Došlo k neočekávané chybě."))
    }
}
