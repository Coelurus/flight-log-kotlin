package eu.profinit.flightlog.controller

import eu.profinit.flightlog.security.AuthService
import eu.profinit.flightlog.security.LoginResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class LoginRequest(
    @field:Email @field:NotBlank @field:Size(max = 254) val email: String,
    @field:NotBlank @field:Size(min = 8, max = 128) val password: String,
    val captchaToken: String? = null,
)

data class ChangePasswordRequest(
    @field:NotBlank val oldPassword: String,
    @field:NotBlank @field:Size(min = 8, max = 128) val newPassword: String,
)

data class PasswordResetRequest(@field:Email @field:NotBlank val email: String)

data class PasswordResetConfirm(
    @field:NotBlank val token: String,
    @field:NotBlank @field:Size(min = 8, max = 128) val newPassword: String,
)

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val auth: AuthService,
) {

    @PostMapping("/login")
    fun login(@RequestBody body: LoginRequest, request: HttpServletRequest): LoginResponse =
        auth.login(request, body.email, body.password, body.captchaToken)

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<Void> {
        request.getSession(false)?.invalidate()
        SecurityContextHolder.clearContext()
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/password/change")
    @PreAuthorize("isAuthenticated()")
    fun changePassword(@RequestBody body: ChangePasswordRequest): ResponseEntity<Void> {
        val email = SecurityContextHolder.getContext().authentication?.name
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        auth.changePassword(email, body.oldPassword, body.newPassword)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/password/reset/request")
    fun requestReset(@RequestBody body: PasswordResetRequest): ResponseEntity<Void> {
        auth.requestPasswordReset(body.email) // never reveal whether the account exists
        return ResponseEntity.accepted().build()
    }

    @PostMapping("/password/reset/confirm")
    fun confirmReset(@RequestBody body: PasswordResetConfirm): ResponseEntity<Void> {
        auth.confirmPasswordReset(body.token, body.newPassword)
        return ResponseEntity.noContent().build()
    }
}
